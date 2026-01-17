# Solutions de Synchronisation PostgreSQL ↔ Neo4j
## Architecture CDC & Outbox Pattern

**Version :** 1.0  
**Date :** 21 Décembre 2025  
**Projet :** Plateforme de Partage de Recettes - RUGE

---

## 1. Problématique et Contexte

### Besoin

Le projet nécessite deux bases de données avec des responsabilités distinctes :
- **PostgreSQL** : Entités métier (Users, Recipes) + Transactions financières
- **Neo4j** : Relations sociales (FOLLOWS, LIKED) + Hiérarchie des recettes

**Défi :** Maintenir la cohérence lors des suppressions sans couplage dans le code métier.

### Besoin Spécifique de Synchronisation

**Nous avons uniquement besoin de synchroniser les DELETE :**
- Quand un User est supprimé de PostgreSQL → Nettoyer Neo4j (supprimer nœud + relations)
- Quand une Recipe est supprimée de PostgreSQL → Nettoyer Neo4j (supprimer nœud + variantes)

**Pas de synchronisation pour les CREATE/UPDATE** : Les relations sociales sont écrites directement dans Neo4j.

---

## 2. Trois Solutions Possibles

Nous étudions trois approches pour gérer cette synchronisation :

### Solution A1 : CDC avec Debezium + Kafka
**Principe :** PostgreSQL émet des événements via Debezium vers Kafka, puis un consumer synchronise Neo4j.

```
┌─────────────┐   WAL   ┌──────────┐   Events   ┌───────┐   Consume   ┌────────┐
│ PostgreSQL  │ ──────> │ Debezium │ ─────────> │ Kafka │ ──────────> │ Neo4j  │
└─────────────┘         └──────────┘            └───────┘             └────────┘
```

**Services nécessaires :** PostgreSQL, Neo4j, Kafka, Zookeeper, Debezium Connect (5 services)

### Solution A2 : CDC avec Debezium Embedded (Sans Kafka)
**Principe :** Debezium est intégré directement dans l'application Java, sans Kafka intermédiaire.

```
┌─────────────┐   WAL   ┌──────────────────────────────────┐
│ PostgreSQL  │ ──────> │  Application Spring Boot         │
└─────────────┘         │  ┌────────────────────────────┐  │
                        │  │  Debezium Embedded Engine  │  │
                        │  └──────────┬─────────────────┘  │
                        │             ↓                     │
                        │  ┌────────────────────────────┐  │
                        │  │  ChangeConsumer (Code)     │  │
                        │  └──────────┬─────────────────┘  │
                        └─────────────┼─────────────────────┘
                                      ↓
                              ┌────────────┐
                              │   Neo4j    │
                              └────────────┘
```

**Services nécessaires :** PostgreSQL, Neo4j (2 services)

### Solution B : Outbox Pattern
**Principe :** Écriture explicite dans une table intermédiaire lors des DELETE.

```
┌─────────────────────────────────────────┐
│  Transaction PostgreSQL (ACID)          │
│  ┌──────────────────────────────────┐  │
│  │ DELETE FROM users WHERE id='abc' │  │
│  │ INSERT INTO outbox_events (...)  │  │
│  └──────────────────────────────────┘  │
└─────────────────────────────────────────┘
              ↓
    ┌─────────────────┐
    │ Outbox Events   │
    │ (Table SQL)     │
    └─────────────────┘
              ↓
    ┌─────────────────┐
    │ Job Polling     │
    │ (@Scheduled)    │
    └─────────────────┘
              ↓
    ┌─────────────────┐
    │     Neo4j       │
    └─────────────────┘
```

**Services nécessaires :** PostgreSQL, Neo4j (2 services)

---

## 3. Séparation des Responsabilités (Commune aux 2 Solutions)

### PostgreSQL stocke :
- Entités complètes (Users, Recipes, Wallets, Transactions)
- Toutes les données volumineuses (photos, instructions, etc.)

### Neo4j stocke :
- Relations uniquement (FOLLOWS, LIKED, IS_VARIANT_OF)
- IDs des nœuds (pas de duplication des données)

### Règle d'Or

> **Aucune table `followers` ou `likes` dans PostgreSQL !**  
> Les relations sociales sont écrites directement dans Neo4j après validation de l'existence des entités dans PostgreSQL.

**Debezium ne surveille que les DELETE** pour nettoyer Neo4j quand une entité est supprimée de PostgreSQL.

---

## 4. Flux de Données (Commun aux 2 Solutions)

### Cas 1 : Follow d'un Utilisateur

```java
@Service
public class SocialService {
    
    public void followUser(String followerId, String followedId) {
        // 1. Vérifier que les users existent dans PostgreSQL
        if (!usersExist(followerId, followedId)) {
            throw new UserNotFoundException();
        }
        
        // 2. Créer la relation directement dans Neo4j
        neo4jTemplate.query("""
            MERGE (follower:User {id: $followerId})
            MERGE (followed:User {id: $followedId})
            CREATE (follower)-[:FOLLOWS {since: datetime()}]->(followed)
            """)
            .bind(followerId).to("followerId")
            .bind(followedId).to("followedId")
            .run();
    }
}
```

**Pas d'événement Kafka** - Écriture directe Neo4j.

### Cas 2 : Suppression d'un Utilisateur

**Flux identique pour les 2 solutions :**

```java
@Service
public class UserService {
    
    @Transactional
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
        
        // Solution A (CDC) : Debezium détecte automatiquement
        // Solution B (Outbox) : On écrit explicitement dans outbox_events
    }
}
```

---

# SOLUTION A1 : CDC avec Debezium + Kafka

## 5A1. Configuration Debezium

### Prérequis PostgreSQL

```sql
-- postgresql.conf
wal_level = logical
```

### Configuration du Connector

```json
{
  "name": "postgres-cleanup-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "debezium",
    "database.password": "dbz_password",
    "database.dbname": "recipe_platform",
    "database.server.name": "recipedb",
    
    "table.include.list": "public.users,public.recipe",
    
    "transforms": "filterDeletes",
    "transforms.filterDeletes.type": "io.debezium.transforms.Filter",
    "transforms.filterDeletes.language": "jsr223.groovy",
    "transforms.filterDeletes.condition": "value.op == 'd'"
  }
}
```

**Filtre au niveau Debezium** : Seuls les DELETE sont envoyés à Kafka.

---

## 6A1. Consumer Kafka

### Configuration Spring Boot

```properties
# application.properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=neo4j-cleanup-group
spring.kafka.consumer.auto-offset-reset=earliest

spring.neo4j.uri=bolt://localhost:7687
spring.neo4j.authentication.username=neo4j
spring.neo4j.authentication.password=password
```

### Implémentation du Consumer

```java
@Service
@Slf4j
public class Neo4jCleanupConsumer {

    @Autowired
    private Neo4jTemplate neo4jTemplate;

    @KafkaListener(
        topics = "recipedb.public.users",
        groupId = "neo4j-cleanup-group"
    )
    public void handleUserDelete(String eventJson) {
        log.info("📨 Événement reçu: {}", eventJson);
        
        try {
            DebeziumEvent event = parseEvent(eventJson);
            String userId = event.getPayload().getBefore().get("id").toString();
            
            // Suppression Neo4j avec toutes les relations
            neo4jTemplate.query("MATCH (u:User {id: $id}) DETACH DELETE u")
                .bind(userId).to("id")
                .run();
            
            log.info("✅ User {} supprimé de Neo4j", userId);
            
        } catch (Exception e) {
            log.error("❌ Erreur traitement", e);
            throw e; // Déclenche le retry
        }
    }
    
    @KafkaListener(
        topics = "recipedb.public.recipe",
        groupId = "neo4j-cleanup-group"
    )
    public void handleRecipeDelete(String eventJson) {
        try {
            DebeziumEvent event = parseEvent(eventJson);
            String recipeId = event.getPayload().getBefore().get("id").toString();
            
            // Suppression avec cascade des variantes
            neo4jTemplate.query("""
                MATCH (r:Recipe {id: $id})
                OPTIONAL MATCH (r)<-[:IS_VARIANT_OF*]-(variant)
                DETACH DELETE r, variant
                """)
                .bind(recipeId).to("id")
                .run();
            
            log.info("✅ Recipe {} supprimée de Neo4j", recipeId);
            
        } catch (Exception e) {
            log.error("❌ Erreur traitement", e);
            throw e;
        }
    }
}
```

**Le consumer se lance automatiquement** au démarrage de Spring Boot grâce à `@KafkaListener`.

---

## 7A1. Gestion des Échecs avec Retry (CDC + Kafka)

### Mécanisme de Retry Automatique

Kafka ne supprime pas les événements après consommation. Il utilise un système d'**offsets** (curseur de lecture).

#### En cas de SUCCÈS :
- L'offset avance automatiquement
- L'événement reste dans Kafka (rétention 7 jours)
- Le consumer passe à l'événement suivant

#### En cas d'ÉCHEC :
- L'exception remonte au container Kafka
- L'offset n'est PAS avancé
- **Retry automatique** avec backoff exponentiel

### Configuration du Retry

```java
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> 
        kafkaListenerContainerFactory() {
        
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        
        // Retry avec backoff exponentiel : 1s, 2s, 4s, 8s, 16s
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new ExponentialBackOff(1000L, 2.0) // Initial 1s, multiplier 2.0
        ));
        
        return factory;
    }
}
```

### Flux de Retry

```
Événement reçu
    ↓
❌ Tentative 1 : Échec Neo4j
    ↓ (attente 1 seconde)
❌ Tentative 2 : Échec Neo4j
    ↓ (attente 2 secondes)
❌ Tentative 3 : Échec Neo4j
    ↓ (attente 4 secondes)
❌ Tentative 4 : Échec Neo4j
    ↓ (attente 8 secondes)
✅ Tentative 5 : Succès !
    ↓
Offset commité, événement suivant
```

**Avantage :** Si Neo4j est temporairement indisponible, le consumer retente automatiquement jusqu'à ce que le service soit de nouveau disponible.

---

## 8A1. Docker Compose (CDC + Kafka)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: recipe_platform
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    command: postgres -c wal_level=logical
    ports:
      - "5432:5432"

  neo4j:
    image: neo4j:5
    environment:
      NEO4J_AUTH: neo4j/password
    ports:
      - "7474:7474"
      - "7687:7687"

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT

  debezium:
    image: debezium/connect:2.4
    depends_on:
      - kafka
      - postgres
    ports:
      - "8083:8083"
    environment:
      BOOTSTRAP_SERVERS: kafka:29092
      GROUP_ID: 1
      CONFIG_STORAGE_TOPIC: debezium_configs
      OFFSET_STORAGE_TOPIC: debezium_offsets
      STATUS_STORAGE_TOPIC: debezium_statuses
```

---

# SOLUTION A2 : CDC avec Debezium Embedded (Sans Kafka)

## 5A2. Principe de Debezium Embedded

**Debezium Embedded** permet d'intégrer le moteur CDC directement dans votre application Java, **sans Kafka**.

### Avantages ✅
- **Simplicité** : 2 services au lieu de 5 (plus de Kafka, Zookeeper, Debezium Connect)
- **Latence réduite** : ~50ms au lieu de ~100ms (pas de transit par Kafka)
- **Moins de ressources** : Mémoire et CPU économisés
- **Debugging plus facile** : Tout dans une seule application

### Inconvénients ⚠️
- **Pas de buffer** : Si l'application est down, les événements sont perdus (mais Debezium reprend où il s'était arrêté au redémarrage)
- **Pas de rejouabilité** : Impossible de rejouer les événements des 7 derniers jours
- **Un seul consumer** : Impossible d'avoir plusieurs applications qui consomment les mêmes événements

---

## 6A2. Dépendances Maven

```xml
<dependencies>
    <!-- Debezium Embedded Engine -->
    <dependency>
        <groupId>io.debezium</groupId>
        <artifactId>debezium-embedded</artifactId>
        <version>2.4.0.Final</version>
    </dependency>
    
    <!-- Debezium PostgreSQL Connector -->
    <dependency>
        <groupId>io.debezium</groupId>
        <artifactId>debezium-connector-postgres</artifactId>
        <version>2.4.0.Final</version>
    </dependency>
    
    <!-- Spring Data Neo4j -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-neo4j</artifactId>
    </dependency>
    
    <!-- Jackson pour parsing JSON -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

---

## 7A2. Configuration Debezium Embedded

```java
@Configuration
@Slf4j
public class DebeziumEmbeddedConfig {

    @Autowired
    private Neo4jTemplate neo4jTemplate;

    @Bean
    public DebeziumEngine<ChangeEvent<String, String>> debeziumEngine() {
        
        // Configuration Debezium
        Properties props = new Properties();
        props.setProperty("name", "postgres-embedded-engine");
        props.setProperty("connector.class", 
            "io.debezium.connector.postgresql.PostgresConnector");
        
        // Stockage des offsets (fichier local)
        props.setProperty("offset.storage", 
            "org.apache.kafka.connect.storage.FileOffsetBackingStore");
        props.setProperty("offset.storage.file.filename", 
            "/tmp/debezium-offsets.dat");
        props.setProperty("offset.flush.interval.ms", "1000");
        
        // PostgreSQL
        props.setProperty("database.hostname", "localhost");
        props.setProperty("database.port", "5432");
        props.setProperty("database.user", "postgres");
        props.setProperty("database.password", "password");
        props.setProperty("database.dbname", "recipe_platform");
        props.setProperty("database.server.name", "recipedb");
        
        // Tables à surveiller
        props.setProperty("table.include.list", "public.users,public.recipe");
        props.setProperty("plugin.name", "pgoutput");
        
        // Créer l'engine
        DebeziumEngine<ChangeEvent<String, String>> engine = 
            DebeziumEngine.create(Json.class)
                .using(props)
                .notifying(record -> {
                    // Callback appelé pour chaque changement
                    handleChangeEvent(record);
                })
                .build();
        
        // Démarrer dans un thread séparé
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(engine);
        
        log.info("✅ Debezium Embedded Engine démarré");
        
        return engine;
    }
    
    private void handleChangeEvent(ChangeEvent<String, String> record) {
        try {
            String value = record.value();
            if (value == null) {
                return; // Tombstone (suppression)
            }
            
            JsonNode root = new ObjectMapper().readTree(value);
            JsonNode payload = root.get("payload");
            String op = payload.get("op").asText();
            
            // Filtrer uniquement les DELETE
            if (!"d".equals(op)) {
                return;
            }
            
            String table = payload.get("source").get("table").asText();
            JsonNode before = payload.get("before");
            
            if ("users".equals(table)) {
                String userId = before.get("id").asText();
                deleteUserFromNeo4j(userId);
                log.info("✅ User {} supprimé de Neo4j", userId);
            } else if ("recipe".equals(table)) {
                String recipeId = before.get("id").asText();
                deleteRecipeFromNeo4j(recipeId);
                log.info("✅ Recipe {} supprimée de Neo4j", recipeId);
            }
            
        } catch (Exception e) {
            log.error("❌ Erreur traitement événement", e);
            // En cas d'erreur, l'événement sera retenté au prochain poll
        }
    }
    
    private void deleteUserFromNeo4j(String userId) {
        neo4jTemplate.query("MATCH (u:User {id: $id}) DETACH DELETE u")
            .bind(userId).to("id")
            .run();
    }
    
    private void deleteRecipeFromNeo4j(String recipeId) {
        neo4jTemplate.query("""
            MATCH (r:Recipe {id: $id})
            OPTIONAL MATCH (r)<-[:IS_VARIANT_OF*]-(variant)
            DETACH DELETE r, variant
            """)
            .bind(recipeId).to("id")
            .run();
    }
}
```

---

## 8A2. Comment Fonctionne Debezium ?

### Lecture du Write-Ahead Log (WAL)

**PostgreSQL ne notifie PAS Debezium.** Debezium lit **activement** le WAL.

```
┌──────────────────────────────────────────────────────────┐
│              PostgreSQL                                   │
│                                                           │
│  Transaction: DELETE FROM users WHERE id='abc'           │
│       ↓                                                   │
│  Write-Ahead Log (WAL)                                   │
│  ┌────────────────────────────────────────────┐         │
│  │ LSN 0/1234: BEGIN                           │         │
│  │ LSN 0/1235: DELETE users id='abc'           │  ←──────┼─── Debezium lit ici
│  │ LSN 0/1236: COMMIT                          │         │     (Logical Replication)
│  └────────────────────────────────────────────┘         │
│                                                           │
└──────────────────────────────────────────────────────────┘
```

### Processus en 4 Étapes

**1. Debezium crée un Replication Slot**

```sql
SELECT * FROM pg_create_logical_replication_slot('debezium_slot', 'pgoutput');
```

Un **replication slot** = bookmark dans le WAL que PostgreSQL conserve.

**2. Debezium streame les changements**

```sql
SELECT * FROM pg_logical_slot_get_changes('debezium_slot', NULL, NULL);
```

PostgreSQL retourne les changements depuis le dernier LSN lu.

**3. Debezium parse et transforme**

```
WAL brut: DELETE FROM users WHERE id='abc'
    ↓
Debezium parse
    ↓
Event JSON:
{
  "op": "d",
  "before": {"id": "abc", "username": "alice"},
  "source": {"table": "users", "lsn": "0/1235"}
}
```

**4. Debezium émet vers la destination**

- **Avec Kafka (A1)** : Écrit dans un topic Kafka
- **Embedded (A2)** : Appelle directement votre callback Java

---

## 9A2. Gestion des Offsets

### Debezium Garde-t-il un Offset ?

**OUI, absolument !** C'est crucial pour ne pas perdre d'événements.

#### Avec Embedded (fichier local)

```properties
offset.storage=org.apache.kafka.connect.storage.FileOffsetBackingStore
offset.storage.file.filename=/tmp/debezium-offsets.dat
```

**Contenu du fichier :**

```json
["recipedb",{"server":"recipedb"}]	{"lsn":"0/1234","txId":5678}
```

**Flux en cas de crash :**

```
1. Debezium lit LSN 0/1234 dans PostgreSQL
2. Traite l'événement (supprime dans Neo4j)
3. Écrit offset dans /tmp/debezium-offsets.dat
4. 💥 Application crash
5. Application redémarre
6. Debezium lit offset depuis le fichier: "LSN 0/1234"
7. Reprend à partir de LSN 0/1235
```

**Pas de perte d'événement**, même en cas de crash.

---

## 10A2. Gestion des Échecs

### Que se Passe-t-il si Neo4j est Down ?

```
Debezium lit un événement DELETE
    ↓
Appelle handleChangeEvent()
    ↓
Essaye deleteUserFromNeo4j()
    ↓
❌ Neo4jException (connexion refusée)
    ↓
Exception loggée
    ↓
Debezium continue avec l'événement suivant
```

**⚠️ IMPORTANT :** Contrairement à Kafka qui retente automatiquement, **Debezium Embedded passe à l'événement suivant**.

### Solution : Implémenter un Retry Manuel

```java
private void handleChangeEvent(ChangeEvent<String, String> record) {
    try {
        // Parse et traite l'événement...
        deleteUserFromNeo4j(userId);
    } catch (Exception e) {
        log.error("❌ Erreur, retry dans 5 secondes...", e);
        
        // Retry avec backoff exponentiel
        retryWithBackoff(() -> deleteUserFromNeo4j(userId), 5);
    }
}

private void retryWithBackoff(Runnable task, int maxRetries) {
    for (int i = 0; i < maxRetries; i++) {
        try {
            task.run();
            return; // Succès
        } catch (Exception e) {
            if (i == maxRetries - 1) {
                log.error("🚨 Abandon après {} tentatives", maxRetries);
                throw e;
            }
            
            long delayMs = (long) Math.pow(2, i) * 1000; // 1s, 2s, 4s, 8s, 16s
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

---

## 11A2. Docker Compose (Simplifié)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: recipe_platform
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    command: postgres -c wal_level=logical
    ports:
      - "5432:5432"

  neo4j:
    image: neo4j:5
    environment:
      NEO4J_AUTH: neo4j/password
    ports:
      - "7474:7474"
      - "7687:7687"
```

**Énorme simplification :** De 5 services à **2 services** !

---

# SOLUTION B : Outbox Pattern

## 5B. Schéma de la Table Outbox

```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL,  -- 'USER', 'RECIPE'
    aggregate_id UUID NOT NULL,            -- ID de l'entité supprimée
    event_type VARCHAR(50) NOT NULL,       -- 'DELETED'
    payload JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0
);

CREATE INDEX idx_outbox_unprocessed ON outbox_events(processed, created_at) 
    WHERE processed = FALSE;
```

---

## 6B. Modification du Service de Suppression

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OutboxEventRepository outboxRepository;

    @Transactional  // Transaction PostgreSQL ACID
    public void deleteUser(String userId) {
        // 1. Suppression de l'entité
        userRepository.deleteById(userId);
        
        // 2. Enregistrement dans l'outbox (MÊME TRANSACTION)
        OutboxEvent event = new OutboxEvent(
            "USER",           // aggregate_type
            userId,           // aggregate_id
            "DELETED",        // event_type
            null              // payload
        );
        outboxRepository.save(event);
        
        // COMMIT atomique : soit les 2 opérations réussissent, soit aucune
    }
}
```

**Garantie ACID** : Si le `save(event)` échoue, le `deleteById(userId)` est rollback automatiquement.

---

## 7B. Entité OutboxEvent

```java
@Entity
@Table(name = "outbox_events")
@Data
public class OutboxEvent {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;
    
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;
    
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
    
    @Column(name = "processed")
    private Boolean processed = false;
    
    @Column(name = "processed_at")
    private Instant processedAt;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    public OutboxEvent() {}
    
    public OutboxEvent(String aggregateType, UUID aggregateId, 
                       String eventType, Map<String, Object> payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
    }
}
```

---

## 8B. Job de Polling et Synchronisation

```java
@Service
@Slf4j
public class OutboxProcessor {

    @Autowired
    private OutboxEventRepository outboxRepository;
    
    @Autowired
    private Neo4jTemplate neo4jTemplate;
    
    private static final int MAX_RETRIES = 5;
    private static final int BATCH_SIZE = 100;

    /**
     * Job exécuté toutes les 500ms
     * Traite les événements non traités par batch de 100
     */
    @Scheduled(fixedDelay = 500)
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = outboxRepository
            .findUnprocessedEventsWithRetryLimit(
                MAX_RETRIES, 
                PageRequest.of(0, BATCH_SIZE)
            );
        
        if (events.isEmpty()) {
            return;
        }
        
        log.info("📦 Traitement de {} événements", events.size());
        
        for (OutboxEvent event : events) {
            try {
                processEvent(event);
                
                event.setProcessed(true);
                event.setProcessedAt(Instant.now());
                outboxRepository.save(event);
                
                log.info("✅ Événement {} traité", event.getId());
                
            } catch (Exception e) {
                log.error("❌ Erreur événement {}", event.getId(), e);
                
                event.setRetryCount(event.getRetryCount() + 1);
                
                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.setProcessed(true);
                    log.error("🚨 Abandon après {} tentatives", MAX_RETRIES);
                }
                
                outboxRepository.save(event);
            }
        }
    }
    
    private void processEvent(OutboxEvent event) {
        if ("USER".equals(event.getAggregateType()) && 
            "DELETED".equals(event.getEventType())) {
            deleteUserFromNeo4j(event.getAggregateId());
        } else if ("RECIPE".equals(event.getAggregateType()) && 
                   "DELETED".equals(event.getEventType())) {
            deleteRecipeFromNeo4j(event.getAggregateId());
        }
    }
    
    private void deleteUserFromNeo4j(UUID userId) {
        neo4jTemplate.query("MATCH (u:User {id: $id}) DETACH DELETE u")
            .bind(userId.toString()).to("id")
            .run();
    }
    
    private void deleteRecipeFromNeo4j(UUID recipeId) {
        neo4jTemplate.query("""
            MATCH (r:Recipe {id: $id})
            OPTIONAL MATCH (r)<-[:IS_VARIANT_OF*]-(variant)
            DETACH DELETE r, variant
            """)
            .bind(recipeId.toString()).to("id")
            .run();
    }
}
```

---

## 9B. Gestion des Échecs avec Retry (Outbox)

### Flux de Retry

```
Événement créé dans outbox (retry_count = 0)
    ↓
❌ Tentative 1 (500ms plus tard) : Échec → retry_count = 1
    ↓
❌ Tentative 2 (500ms plus tard) : Échec → retry_count = 2
    ↓
❌ Tentative 3 (500ms plus tard) : Échec → retry_count = 3
    ↓
❌ Tentative 4 (500ms plus tard) : Échec → retry_count = 4
    ↓
❌ Tentative 5 (500ms plus tard) : Échec → retry_count = 5
    ↓
🚨 Abandon : processed = true + alerte admin
```

---

## 10B. Nettoyage de la Table Outbox

```java
@Service
public class OutboxCleanup {

    @Autowired
    private OutboxEventRepository outboxRepository;

    /**
     * Nettoie les événements traités de plus de 7 jours
     * Exécuté tous les jours à 3h du matin
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldEvents() {
        Instant cutoffDate = Instant.now().minus(7, ChronoUnit.DAYS);
        
        int deleted = outboxRepository
            .deleteByProcessedTrueAndProcessedAtBefore(cutoffDate);
        
        log.info("🧹 Nettoyage : {} événements supprimés", deleted);
    }
}
```

---

## 11B. Docker Compose (Outbox)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: recipe_platform
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"

  neo4j:
    image: neo4j:5
    environment:
      NEO4J_AUTH: neo4j/password
    ports:
      - "7474:7474"
      - "7687:7687"
```

**Beaucoup plus simple** : Seulement 2 services au lieu de 5 !

---

# COMPARAISON DES SOLUTIONS

## 9. Tableau Comparatif

| Critère | Solution A1 (CDC + Kafka) | Solution A2 (CDC Embedded) | Solution B (Outbox) |
|---------|---------------------------|----------------------------|---------------------|
| **Complexité déploiement** | ⚠️ 5 services | ✅ 2 services | ✅ 2 services |
| **Dépendances externes** | ❌ Kafka, Zookeeper, Debezium | ✅ Aucune (Debezium intégré) | ✅ Aucune |
| **Code intrusif** | ✅ Non (CDC transparent) | ✅ Non (CDC transparent) | ⚠️ Oui (INSERT outbox) |
| **Latence** | ⚠️ ~100ms | ✅ ~50ms | ✅ ~500ms |
| **Scalabilité** | ✅✅ Excellente (millions/sec) | ⚠️ Bonne (<10k/sec) | ⚠️ Moyenne (<1k/sec) |
| **Rejouabilité** | ✅ Oui (7 jours) | ❌ Non | ❌ Non |
| **Buffer si app down** | ✅ Oui (Kafka) | ❌ Non | ✅ Oui (table outbox) |
| **Multiple consumers** | ✅ Oui | ❌ Non | ❌ Non |
| **Debugging** | ⚠️ Topics Kafka | ✅ Logs application | ✅ Table SQL |
| **Retry automatique** | ✅ Oui (Kafka) | ⚠️ À implémenter | ⚠️ À implémenter |
| **Charge CPU** | ✅ Push events | ✅ Push events | ⚠️ Polling continu |
| **Apprentissage** | ⚠️ Kafka, Debezium | ⭐⭐ Debezium only | ⭐ SQL, Spring |
| **Pertinence académique** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |

---

## 10. Recommandation

### Choisir Solution A1 (CDC + Kafka) si :

✅ Démonstration d'une **architecture professionnelle** (GAFAM-style)  
✅ Besoin de **rejouabilité** (replay des 7 derniers jours)  
✅ **Multiple consumers** (Analytics, Audit, etc.)  
✅ Valorisation académique **maximale**  
✅ Vous avez du **temps** (configuration Kafka)

### Choisir Solution A2 (CDC Embedded) si :

✅ Simplicité **prioritaire** (2 services seulement)  
✅ Latence **minimale** (~50ms)  
✅ **Un seul consumer** (Neo4j sync)  
✅ Pas besoin de rejouabilité  
✅ **Meilleur compromis** pour projet académique ⭐

### Choisir Solution B (Outbox) si :

✅ Équipe **débutante** sur Kafka et Debezium  
✅ Besoin de **debugger facilement** (requêtes SQL)  
✅ Volume **très faible** de suppressions  
✅ Contrainte de **temps extrême** (déploiement rapide)  
✅ Prototype / MVP

---

## 11. Résumé

### Points Communs aux Solutions

✅ **PostgreSQL** = Source de vérité pour les entités  
✅ **Neo4j** = Source de vérité pour les relations  
✅ **Synchronisation asynchrone** des DELETE uniquement  
✅ **Retry en cas d'échec** Neo4j

### Solution A1 (CDC + Kafka) : Architecture Événementielle Distribuée

```
DELETE PostgreSQL → Debezium → Kafka → Consumer → Neo4j
```

**Forces :** Découplage total, buffer Kafka, rejouabilité, multiple consumers  
**Faiblesses :** Complexité (5 services), latence (~100ms), courbe d'apprentissage

### Solution A2 (CDC Embedded) : Architecture Événementielle Intégrée

```
DELETE PostgreSQL → Debezium Embedded (App Java) → Neo4j
```

**Forces :** Simplicité (2 services), latence minimale (~50ms), CDC transparent  
**Faiblesses :** Pas de buffer externe, pas de rejouabilité, un seul consumer

### Solution B (Outbox) : Polling Database

```
DELETE PostgreSQL + INSERT outbox → Job polling → Neo4j
```

**Forces :** Simplicité maximale, debugging facile (SQL), 2 services  
**Faiblesses :** Code intrusif, polling CPU, pas de rejouabilité

---

## 12. Stratégie Recommandée pour Projet Académique

### Approche Progressive

**Phase 1 (Semaine 1-2) : Commencer par A2 (Embedded)**
- ✅ Fonctionnel rapidement
- ✅ 2 services seulement
- ✅ Architecture CDC déjà valorisante

**Phase 2 (Semaine 3-4) : Si temps disponible, migrer vers A1 (Kafka)**
- ✅ Upgrade pour impressionner le jury
- ✅ Démontre maîtrise d'architectures avancées
- ✅ Fallback possible sur A2 si manque de temps

**Avantage :** Vous avez toujours une solution fonctionnelle, et vous upgradez si possible.

---

# AMÉLIORATION SOLUTION A1 : Spring Cloud Stream

## 13. Intégration de Spring Cloud Stream

### Qu'est-ce que Spring Cloud Stream ?

**Spring Cloud Stream** est une abstraction Spring au-dessus de Kafka qui simplifie le code en cachant la complexité des consumers.

**Avantages :**
- ✅ Code plus propre et fonctionnel
- ✅ Configuration déclarative (YAML)
- ✅ Retry et DLQ intégrés automatiquement
- ✅ Abstraction du broker (peut changer Kafka → RabbitMQ sans modifier le code)

---

## 14. Dépendances Maven

```xml
<dependencies>
    <!-- Spring Cloud Stream -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-stream</artifactId>
    </dependency>
    
    <!-- Kafka Binder -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-stream-binder-kafka</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-neo4j</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 15. Configuration Spring Cloud Stream

### application.yml

```yaml
spring:
  cloud:
    stream:
      bindings:
        # Consumer User DELETE
        handleUserDelete-in-0:
          destination: recipedb.public.users
          group: neo4j-cleanup-group
          consumer:
            max-attempts: 5
            back-off-initial-interval: 1000
            back-off-multiplier: 2.0
            back-off-max-interval: 10000
        
        # Consumer Recipe DELETE
        handleRecipeDelete-in-0:
          destination: recipedb.public.recipe
          group: neo4j-cleanup-group
          consumer:
            max-attempts: 5
            back-off-initial-interval: 1000
            back-off-multiplier: 2.0
      
      kafka:
        binder:
          brokers: localhost:9092
        bindings:
          handleUserDelete-in-0:
            consumer:
              enable-dlq: true
              dlq-name: recipedb.public.users.dlq
          handleRecipeDelete-in-0:
            consumer:
              enable-dlq: true
              dlq-name: recipedb.public.recipe.dlq
  
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: password
```

**Convention de nommage :**
- `handleUserDelete-in-0` : 
  - `handleUserDelete` = nom de la fonction
  - `in` = input (consumer)
  - `0` = index

---

## 16. Implémentation avec Functions

```java
@Configuration
@Slf4j
public class Neo4jSyncConsumer {

    @Autowired
    private Neo4jTemplate neo4jTemplate;

    /**
     * Consumer pour DELETE User
     * Spring Cloud Stream invoque automatiquement cette fonction
     */
    @Bean
    public Consumer<Message<String>> handleUserDelete() {
        return message -> {
            String eventJson = message.getPayload();
            log.info("📨 Événement User: {}", eventJson);
            
            try {
                DebeziumEvent event = parseEvent(eventJson);
                
                if (!"d".equals(event.getPayload().getOp())) {
                    return;
                }
                
                String userId = event.getPayload().getBefore()
                    .get("id").toString();
                
                neo4jTemplate.query(
                    "MATCH (u:User {id: $id}) DETACH DELETE u"
                )
                    .bind(userId).to("id")
                    .run();
                
                log.info("✅ User {} supprimé", userId);
                
            } catch (Exception e) {
                log.error("❌ Erreur", e);
                throw new RuntimeException("Échec sync Neo4j", e);
            }
        };
    }

    /**
     * Consumer pour DELETE Recipe
     */
    @Bean
    public Consumer<Message<String>> handleRecipeDelete() {
        return message -> {
            String eventJson = message.getPayload();
            log.info("📨 Événement Recipe: {}", eventJson);
            
            try {
                DebeziumEvent event = parseEvent(eventJson);
                
                if (!"d".equals(event.getPayload().getOp())) {
                    return;
                }
                
                String recipeId = event.getPayload().getBefore()
                    .get("id").toString();
                
                neo4jTemplate.query("""
                    MATCH (r:Recipe {id: $id})
                    OPTIONAL MATCH (r)<-[:IS_VARIANT_OF*]-(variant)
                    DETACH DELETE r, variant
                    """)
                    .bind(recipeId).to("id")
                    .run();
                
                log.info("✅ Recipe {} supprimée", recipeId);
                
            } catch (Exception e) {
                log.error("❌ Erreur", e);
                throw new RuntimeException("Échec sync Neo4j", e);
            }
        };
    }
    
    private DebeziumEvent parseEvent(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, DebeziumEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur parsing", e);
        }
    }
}
```

---

## 17. Comparaison @KafkaListener vs Spring Cloud Stream

| Aspect | @KafkaListener | Spring Cloud Stream |
|--------|----------------|---------------------|
| **Configuration** | Java verbose (KafkaConsumerConfig) | YAML déclaratif |
| **Retry** | ErrorHandler manuel | Intégré (max-attempts) |
| **DLQ** | DeadLetterPublisher manuel | Intégré (enable-dlq) |
| **Code métier** | ⚠️ Mélangé avec infra | ✅ Function pure |
| **Abstraction** | Couplé à Kafka | Indépendant du broker |
| **Lignes de code** | ~100 lignes | ~50 lignes |

### Code Comparé

**Avant (@KafkaListener) :**
```java
@Service
public class Consumer {
    @KafkaListener(topics = "users", groupId = "group")
    public void handle(String msg) { }
}

// + KafkaConsumerConfig (50 lignes)
// + ErrorHandler config (30 lignes)
```

**Après (Spring Cloud Stream) :**
```java
@Configuration
public class Consumer {
    @Bean
    public Consumer<Message<String>> handleUserDelete() {
        return msg -> { };
    }
}

// Configuration dans application.yml (15 lignes)
```

---

## 18. Monitoring avec Spring Cloud Stream

Spring Cloud Stream expose automatiquement des endpoints :

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,bindings
```

```bash
# État des bindings
curl http://localhost:8080/actuator/bindings

# Santé
curl http://localhost:8080/actuator/health
```

Réponse :
```json
{
  "status": "UP",
  "components": {
    "binders": {
      "status": "UP",
      "details": {
        "kafka": {
          "status": "UP"
        }
      }
    }
  }
}
```

---

## 19. Choix entre @KafkaListener et Spring Cloud Stream

### Utiliser Spring Cloud Stream si :

✅ Vous voulez **du code plus propre** (séparation infra/métier)  
✅ Vous utilisez déjà **Spring Cloud** dans le projet  
✅ Vous préférez la **configuration déclarative** (YAML)  
✅ Vous voulez **l'abstraction** (changer de broker facilement)  
✅ **Projet académique** (démontre connaissance Spring avancé)

### Utiliser @KafkaListener si :

✅ Vous avez besoin de **contrôle fin** sur Kafka  
✅ Votre équipe **maîtrise Kafka** en profondeur  
✅ Vous ne voulez **pas d'abstraction** supplémentaire  
✅ Projet **simple** sans écosystème Spring Cloud

---

## 20. Résumé Final des Options

| Solution | Complexité | Code | Déploiement | Latence | Recommandation |
|----------|------------|------|-------------|---------|----------------|
| **A1a. CDC + @KafkaListener** | ⭐⭐⭐ | Verbeux | 5 services | ~100ms | Production classique |
| **A1b. CDC + Spring Cloud Stream** | ⭐⭐ | Concis | 5 services | ~100ms | Projet académique avancé |
| **A2. CDC Embedded** | ⭐⭐ | Moyen | 2 services | ~50ms | **Meilleur compromis** ✅ |
| **B. Outbox Pattern** | ⭐ | Simple | 2 services | ~500ms | Prototype rapide |

**Recommandation finale pour votre projet :**  

**Solution A2 (CDC Embedded)** comme baseline, puis upgrade vers **A1b (CDC + Spring Cloud Stream)** si temps disponible.

Cette approche combine :
- Fonctionnalité rapide (A2 en 1 semaine)
- Architecture professionnelle (CDC)
- Possibilité d'upgrade (A1b pour impressionner)
- Fallback sécurisé (A2 reste fonctionnel)