# Rapport d'Étude : Architecture de Données pour la Plateforme de Partage de Recettes

## 1. Introduction et Contexte du Projet

Le projet consiste à développer une plateforme collaborative de partage de recettes intégrant trois dimensions fonctionnelles aux exigences contradictoires :

- **Dimension financière** : Gestion rigoureuse des transactions monétaires avec un système de porte-monnaie virtuel, redistribution mensuelle des revenus et intégrité absolue des soldes (propriétés ACID strictes)
- **Dimension structurelle** : Gestion d'une hiérarchie complexe de recettes et variantes formant des arbres de profondeur indéfinie, nécessitant des traversées récursives pour l'affichage et les calculs financiers
- **Dimension sociale** : Génération de fils d'actualité personnalisés basés sur un graphe social (utilisateurs suivis et utilisateurs suivis par ceux-ci), avec des volumes potentiellement massifs

L'équipe a identifié que ces trois exigences s'opposent traditionnellement dans le choix d'une architecture de données unique. Ce rapport présente une analyse comparative approfondie entre deux approches architecturales : une solution purement relationnelle (PostgreSQL) et une solution hybride (PostgreSQL + Neo4j).

## 2. Analyse Détaillée par Domaine Fonctionnel

### 2.1 Gestion de l'Arborescence des Variantes de Recettes

#### 2.1.1 Problématique Métier

Le système doit gérer une structure hiérarchique où une recette peut être une variante d'une autre recette, elle-même potentiellement variante d'une troisième, et ainsi de suite sans limite de profondeur. Cette structure présente deux défis majeurs :

1. **Affichage des différences** : Pour afficher une variante, le système doit remonter toute la chaîne jusqu'à la recette de base, identifier les modifications à chaque niveau (ingrédients ajoutés/retirés, quantités modifiées, temps ajusté) et présenter visuellement ces changements
2. **Calcul de redistribution financière** : Lors de la redistribution mensuelle des super-likes, le système doit parcourir récursivement la chaîne de variantes en appliquant la règle de partage 50/50 à chaque niveau

#### 2.1.2 Solution Relationnelle Pure (PostgreSQL)

**Structure de données** : L'équipe utilise une table `Recipe` avec une clé étrangère auto-référentielle `parent_id` permettant de modéliser l'arbre.

```sql
CREATE TABLE recipe (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    parent_id UUID REFERENCES recipe(id),
    ingredients JSONB,
    preparation_time INTEGER,
    instructions TEXT,
    photo BYTEA,
    INDEX idx_parent (parent_id)
);
```

**Implémentation des traversées** : Pour reconstituer la chaîne de parenté, PostgreSQL propose les Common Table Expressions (CTE) récursives via `WITH RECURSIVE` :

```sql
WITH RECURSIVE recipe_chain AS (
    SELECT id, parent_id, title, ingredients, 0 AS depth
    FROM recipe
    WHERE id = :variant_id
    
    UNION ALL
    
    SELECT r.id, r.parent_id, r.title, r.ingredients, rc.depth + 1
    FROM recipe r
    INNER JOIN recipe_chain rc ON r.id = rc.parent_id
)
SELECT * FROM recipe_chain ORDER BY depth DESC;
```

**Analyse de complexité algorithmique** :

- **Complexité** : Pour une chaîne de profondeur *h* dans une base de *n* recettes, chaque niveau de la récursion nécessite une recherche dans l'index B-tree sur `parent_id`. La complexité est donc **O(h × log n)** où :
  - *h* est la hauteur de l'arbre de variantes (nombre de niveaux à remonter)
  - *log n* est le coût d'accès à l'index B-tree pour trouver le parent

- **Facteurs de coût supplémentaires** :
  - Le moteur SQL doit maintenir en mémoire l'ensemble des résultats intermédiaires de la récursion
  - Pour des recettes volumineuses (photos, instructions détaillées), le transfert de *h* entités complètes représente un volume de données important
  - Les CTE récursives sont difficiles à optimiser par le planificateur de requêtes

- **Calcul des différences** : Une fois la chaîne récupérée, l'application doit charger toutes les entités avec tous leurs attributs pour calculer les différences niveau par niveau, ce qui complexifie la logique métier

**Points faibles identifiés** :

1. Le coût augmente proportionnellement à la profondeur *h* et logarithmiquement à la taille de la base *n*
2. Les données transférées sont souvent redondantes (attributs identiques à chaque niveau)
3. La logique de calcul des différences est déportée dans la couche application
4. En cas de forte concurrence, les ressources CPU et mémoire sont fortement sollicitées

#### 2.1.3 Solution Hybride (PostgreSQL + Neo4j)

**Principe de séparation** : L'équipe adopte une stratégie de stockage minimal dans Neo4j. Seuls les **identifiants et métadonnées légères** sont dupliqués dans le graphe, tandis que PostgreSQL conserve toutes les données volumineuses.

**Structure de données** :

- **PostgreSQL** : Stocke TOUTES les données (ingrédients complets, instructions, photos, quantités détaillées)
- **Neo4j** : Stocke uniquement les **IDs**, titres et la structure relationnelle

```cypher
// Nœud Recipe dans Neo4j (version MINIMALE)
CREATE (r:Recipe {
    id: 'uuid-123',
    title: 'Tarte aux pommes revisitée'
})

// Relation de variante avec métadonnées de différence
CREATE (variant:Recipe)-[:IS_VARIANT_OF {
    addedIngredients: ['sucre vanillé'],
    removedIngredients: [],
    modifiedQuantities: [{'ingredient': 'sucre', 'change': '+50g'}]
}]->(base:Recipe)
```

**Flux de récupération en deux étapes** :

```
1. Requête Neo4j → Récupération de la chaîne d'IDs (rapide)
2. Requête PostgreSQL → Batch query sur les IDs récupérés (rapide via index)
```

**Implémentation des traversées** :

```cypher
// Étape 1 : Récupération de la structure dans Neo4j
MATCH path = (variant:Recipe)-[:IS_VARIANT_OF*]->(base:Recipe)
WHERE variant.id = $variantId
RETURN [node IN nodes(path) | node.id] AS recipeIds,
       relationships(path) AS modifications
ORDER BY length(path) DESC
LIMIT 1
```

Puis en Java :
```
Étape 2 : Récupération des données complètes dans PostgreSQL
  - SELECT * FROM recipe WHERE id IN (id1, id2, id3, ...)
  - Utilisation de l'index sur PRIMARY KEY → O(k × log n) où k = nombre d'IDs
```

**Analyse de Complexité et Gain de Performance** :

Le gain principal repose sur le mécanisme d'**adjacence sans index** (index-free adjacency) propre aux bases de données orientées graphe.

- **En architecture Relationnelle (SQL)** : La reconstruction de l'arbre nécessite une recherche d'index à chaque niveau de récursion. La complexité temporelle est de **O(k × log N)**, où *k* est la profondeur de la variante et *N* le nombre total de recettes. La performance se dégrade mécaniquement à mesure que la base de données grandit (passage à l'échelle difficile).

- **En architecture Graphe (Neo4j)** : La traversée se fait par déréférencement direct de pointeurs mémoire, sans scanner d'index. La complexité est de **O(k)**. Le temps d'exécution dépend uniquement de la profondeur de la variante, et est totalement indépendant du volume total de données (*N*).

**Conclusion** : Cette complexité linéaire **O(k)** garantit que l'affichage des variantes et le calcul de redistribution resteront instantanés (quelques millisecondes) même avec des millions de recettes, validant le critère de scalabilité du sujet.

**Avantages clés de l'approche hybride** :

1. **Performance constante** : Le temps de traversée Neo4j ne dépend que de *k*, pas de *N* (propriété d'index-free adjacency)
2. **Données minimales dans Neo4j** : Pas de duplication massive, maintenance simplifiée
3. **Simplicité du code** : La logique de traversée est déclarative en Cypher
4. **Métadonnées sur les relations** : Les modifications sont stockées directement sur [:IS_VARIANT_OF], évitant les calculs applicatifs

### 2.2 Génération du Fil d'Actualité Social (Feed)

#### 2.2.1 Problématique Métier : Le défi de la volumétrie

Le système doit agréger les contenus provenant du cercle social élargi de l'utilisateur : ses abonnements directs (Niveau 1) et les abonnements de ces derniers (Niveau 2).

Dans un scénario réaliste avec 100 000 utilisateurs suivant chacun 50 personnes, le calcul du niveau 2 impose de vérifier 2 500 sources potentielles (50 × 50) pour chaque requête. La contrainte est ici la latence : le fil doit se charger en temps réel, indépendamment de la charge du serveur.

#### 2.2.2 Solution Relationnelle Pure (PostgreSQL)

**Mécanisme technique** :

Le modèle relationnel s'appuie sur des tables de jointures (User ⇄ Follows ⇄ User). Pour reconstituer le réseau de niveau 2, le moteur de base de données doit effectuer des opérations de jointures imbriquées.

**Structure de données** :

```sql
CREATE TABLE followers (
    follower_id UUID REFERENCES users(id),
    followed_id UUID REFERENCES users(id),
    created_at TIMESTAMP,
    PRIMARY KEY (follower_id, followed_id)
);

CREATE INDEX idx_followers_follower ON followers(follower_id);
CREATE INDEX idx_followers_followed ON followers(followed_id);
```

**Schéma logique** :

```
Utilisateur → Recherche Indexée (O(log N)) → Abonnements N1 
            → Recherche Indexée ×50 → Abonnements N2
```

**Analyse de complexité** :

L'architecture repose sur des arbres de recherche (B-Trees). La complexité de l'opération est proportionnelle à **O(k · log N)**, où *N* est le nombre total d'utilisateurs dans la base et *k* le nombre d'amis.

Le facteur **log N** est critique : cela signifie que le temps de calcul augmente mathématiquement à mesure que la base de données grossit. Les jointures imbriquées multiplient ce coût, créant une latence structurelle inévitable à grande échelle.

**Points de contention** :

- Jointures multiples nécessitant des parcours d'index B-tree répétés
- Opération DISTINCT coûteuse pour éliminer les doublons au niveau 2
- Dépendance structurelle à la taille *N* de la base (facteur log N)

#### 2.2.3 Solution Hybride (PostgreSQL + Neo4j)

**Mécanisme technique** :

La base de données graphe utilise le principe d'**adjacence sans index** (Index-Free Adjacency). Concrètement, chaque nœud utilisateur contient physiquement l'adresse mémoire de ses voisins.

**Structure de données** :

- **Neo4j** : Nœuds User (id, username) + Relations [:FOLLOWS], [:PUBLISHED]
- **PostgreSQL** : Données complètes des recettes

```cypher
// Structure minimale dans Neo4j
CREATE (u:User {id: 'uuid', username: 'alice'})
CREATE (follower:User)-[:FOLLOWS {since: datetime()}]->(followed:User)
CREATE (author:User)-[:PUBLISHED {date: datetime()}]->(recipe:Recipe {id: 'recipe-uuid'})
```

**Schéma logique** :

```
Utilisateur → Pointeur Direct (O(1)) → Abonnements N1 
            → Pointeur Direct (O(1)) → Abonnements N2
```

**Analyse de complexité** :

La traversée du graphe s'effectue en temps constant par relation, soit une complexité de **O(k)**.

Le point décisif est l'absence du facteur *N* : la performance dépend uniquement du nombre de voisins à récupérer (*k*), et est totalement indépendante du volume total de la base (*N*). Que la plateforme héberge 100 000 ou 10 millions d'utilisateurs, la récupération du flux social s'exécute avec la même rapidité (quelques millisecondes).

**Flux de récupération en deux étapes** :

```
Étape 1 (Neo4j) : Calcul du graphe social et récupération des IDs de recettes
Étape 2 (PostgreSQL) : Récupération des données complètes des recettes
```

**Implémentation Cypher** (Étape 1) :

```cypher
// Requête Neo4j : récupère uniquement les IDs
MATCH (me:User {id: $userId})-[:FOLLOWS*1..2]->(author:User)
      -[:PUBLISHED]->(recipe:Recipe)
WHERE recipe.publishedAt >= datetime() - duration({days: 30})
WITH recipe.id AS recipeId,
     length((me)-[:FOLLOWS*1..2]->(author)) AS socialDistance
RETURN recipeId, socialDistance
ORDER BY socialDistance ASC
```

**Implémentation SQL** (Étape 2) :

```sql
SELECT r.*, COUNT(l.id) + COUNT(sl.id) * 2 AS popularity
FROM recipes r
LEFT JOIN likes l ON l.recipe_id = r.id
LEFT JOIN super_likes sl ON sl.recipe_id = r.id
WHERE r.id IN (:recipeIds)  -- IDs récupérés de Neo4j
GROUP BY r.id
ORDER BY popularity DESC
```

**Comparaison asymptotique** :

| Approche | Complexité | Dépendance à N | Scalabilité |
|----------|------------|----------------|-------------|
| PostgreSQL seul | O(k · log N) | ✗ Oui (log N) | ⚠️ Dégradation avec volume |
| Neo4j + PostgreSQL | O(k) | ✓ Non | ✅ Performance stable |

**Propriété de scalabilité fondamentale** : Le temps de réponse de Neo4j dépend de la taille du voisinage local (*k*), **pas de la taille totale de la base de données** (*N*). Cette propriété d'index-free adjacency garantit que les performances restent constantes même avec une croissance exponentielle du nombre d'utilisateurs.

### 2.3 Gestion Financière et Transactionnelle

#### 2.3.1 Problématique Métier

Le système doit garantir une intégrité absolue sur trois types de porte-monnaie :

1. Porte-monnaie utilisateurs (rechargeable par carte bancaire, débit lors des super-likes)
2. Porte-monnaie des bénéfices (reçoit 40% de chaque super-like)
3. Porte-monnaie de redistribution (reçoit 60%, redistribué mensuellement)

Les contraintes sont critiques :

- **Atomicité** : Un super-like doit débiter l'utilisateur, créditer les deux porte-monnaie système, et créer un enregistrement de transaction, le tout ou rien
- **Isolation** : Deux super-likes simultanés ne doivent jamais créer de "race condition" (solde négatif)
- **Cohérence** : La somme totale de tous les porte-monnaie doit être constante à tout instant (conservation de l'argent)
- **Durabilité** : Aucune transaction validée ne peut être perdue, même en cas de crash serveur

#### 2.3.2 Solution Relationnelle (PostgreSQL) - L'État de l'Art

**Structure de données** :

```sql
CREATE TABLE wallet (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE REFERENCES users(id),
    balance INTEGER NOT NULL CHECK (balance >= 0), -- en centimes
    version INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE transaction (
    id UUID PRIMARY KEY,
    source_wallet_id UUID REFERENCES wallet(id),
    destination_wallet_id UUID REFERENCES wallet(id),
    amount INTEGER NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    reference_id UUID,
    created_at TIMESTAMP DEFAULT NOW()
);
```

**Flux d'exécution pour un super-like** :

```
1. Début transaction PostgreSQL (@Transactional)
2. Appel API Stripe pour débiter la carte bancaire (recharge) ou vérifier le solde
3. SELECT ... FOR UPDATE sur wallet utilisateur (verrouillage pessimiste)
4. Vérification balance >= coût
5. UPDATE balance - coût (débit du porte-monnaie virtuel)
6. UPDATE porte-monnaie bénéfices + (coût × 40%)
7. UPDATE porte-monnaie redistribution + (coût × 60%)
8. INSERT traces dans table transaction (audit et traçabilité)
9. COMMIT (ou ROLLBACK si exception)
```

**Rôle de PostgreSQL** :

PostgreSQL ne gère pas directement les paiements bancaires (c'est le rôle de Stripe), mais assure :

1. **Stockage persistant** : Conservation de l'historique complet des transactions et des soldes
2. **Traçabilité** : Chaque mouvement d'argent est enregistré avec horodatage, type, référence
3. **Cohérence transactionnelle** : Garantie ACID que tous les débits/crédits sont atomiques
4. **Contraintes d'intégrité** : Vérification au niveau base de données que `balance >= 0`
5. **Isolation concurrentielle** : Gestion des accès simultanés via verrouillage (SELECT FOR UPDATE)

**Mécanismes de sécurité PostgreSQL** :

1. **SELECT FOR UPDATE** : Verrouille les lignes lues, empêchant toute modification concurrente
2. **Isolation SERIALIZABLE** : Garantit l'exécution séquentielle logique des transactions
3. **Contrainte CHECK** : Vérification `balance >= 0` au niveau base de données
4. **Write-Ahead Log (WAL)** : Durabilité garantie, aucune transaction validée ne peut être perdue

**Verdict** : PostgreSQL excelle ici. C'est sa raison d'être historique. Les décennies d'optimisation sur les transactions ACID en font l'outil le plus fiable pour la **gestion de l'état financier** (stockage, cohérence, traçabilité), tandis que Stripe gère les paiements bancaires réels.

#### 2.3.3 Position de Neo4j - Limitations Reconnues

Neo4j propose des transactions ACID, mais avec des limitations importantes pour la gestion financière :

**Problèmes identifiés** :

1. **Pas de contraintes d'intégrité fortes** : Pas de support natif pour `CHECK (balance >= 0)` - vérification applicative nécessaire
2. **Isolation limitée** : READ_COMMITTED par défaut, pas de SERIALIZABLE natif
3. **Pas de verrouillage explicite** : Pas d'équivalent à `SELECT FOR UPDATE`
4. **Audit trail complexe** : Création de nœuds Transaction alourdit le graphe sans bénéfice

**Recommandation de l'équipe** : **Ne jamais gérer l'argent dans Neo4j**. Cette règle est absolue dans l'architecture hybride. Toutes les opérations financières (wallets, transactions, recharges, débits) restent exclusivement dans PostgreSQL.

### 2.4 Calcul de Redistribution Mensuelle

#### 2.4.1 Algorithme de Redistribution

L'équipe doit, chaque fin de mois, redistribuer le contenu du porte-monnaie de redistribution vers les auteurs de recettes selon la règle 50/50 récursive le long des chaînes de variantes.

#### 2.4.2 Implémentation Relationnelle Pure

```
Pour chaque recette ayant reçu des super-likes:
  1. Exécuter CTE récursive → Obtenir chaîne de variantes
     Complexité : O(h × log n) par recette
  2. Calculer les montants à redistribuer (50/50 récursif)
  3. Effectuer les virements dans PostgreSQL
  
Complexité totale : O(R × h × log n)
où R = nombre de recettes avec super-likes
```

Avec R = 1000 recettes et h = 5 en moyenne, cela représente 5000 CTE récursives, chacune nécessitant plusieurs accès à l'index B-tree.

#### 2.4.3 Implémentation Hybride

```
Étape 1 (Neo4j) : Calcul en batch de toutes les chaînes
  - Une seule requête Cypher récupère toutes les structures
  - Complexité : O(R × h) avec facteur constant faible
  - Résultat : Liste des plans de redistribution (IDs des auteurs + montants)

Étape 2 (PostgreSQL) : Exécution des virements
  - Pour chaque auteur, UPDATE wallet
  - INSERT traces d'audit
  - Complexité : O(A × log n) où A = nombre d'auteurs à rémunérer
  
Complexité totale : O(R × h) + O(A × log n)
```

**Justification théorique du gain** : 
- Élimination de R CTE récursives coûteuses (R × O(h × log n))
- Une seule traversée de graphe pour toutes les recettes
- Batch processing vs itération séquentielle

En pratique, il faudrait mesurer le temps réel d'exécution pour valider l'ampleur du gain, mais théoriquement l'approche hybride évite de répéter R fois la même logique de traversée.

## 3. Synchronisation et Cohérence entre PostgreSQL et Neo4j

### 3.1 Principe Fondamental : UUID et Pattern Maître-Réplique

L'équipe adopte une stratégie de synchronisation simple basée sur deux piliers :

1. **Génération d'UUID côté Java** : Tous les IDs sont générés en Java avant insertion dans les bases, garantissant l'unicité et la cohérence des identifiants entre PostgreSQL et Neo4j

2. **PostgreSQL comme source de vérité** : Toute création/modification commence par PostgreSQL, puis est propagée vers Neo4j

**Règle d'or** : Si PostgreSQL échoue, on ne touche pas à Neo4j. Si Neo4j échoue après PostgreSQL, on lève une exception pour déclencher le rollback de la transaction PostgreSQL.

### 3.2 Flux de Synchronisation par Cas d'Usage

#### 3.2.1 Inscription d'un Utilisateur

```
Flux d'exécution:
1. Génération UUID en Java (sharedId)
2. @Transactional → Début transaction PostgreSQL
3. INSERT User dans PostgreSQL (email, password, id=sharedId)
4. INSERT Wallet dans PostgreSQL (user_id=sharedId, balance=0)
5. INSERT UserNode dans Neo4j (id=sharedId, username)
6. Si Neo4j échoue → Exception → Rollback PostgreSQL
7. Si tout OK → COMMIT PostgreSQL
```

**Données stockées** :
- PostgreSQL : Toutes les données (email, password_hash, profil complet, wallet)
- Neo4j : ID + username uniquement (pour l'affichage dans le graphe social)

#### 3.2.2 Publication d'une Recette

```
Flux d'exécution:
1. Génération UUID en Java (recipeId)
2. @Transactional → Début transaction PostgreSQL
3. INSERT Recipe dans PostgreSQL (id=recipeId, title, ingredients JSONB, 
   instructions TEXT, photo BYTEA, preparation_time, author_id)
4. INSERT RecipeNode dans Neo4j (id=recipeId, title UNIQUEMENT)
5. CREATE relation (:User)-[:PUBLISHED]->(:Recipe) dans Neo4j
6. Si variante: CREATE (:Recipe)-[:IS_VARIANT_OF {modifications}]->(:Recipe)
7. Si Neo4j échoue → Exception → Rollback PostgreSQL
8. Si tout OK → COMMIT PostgreSQL
```

**Principe de minimalisme** : Neo4j ne contient que l'ID et le titre. Toutes les données volumineuses (ingrédients, instructions, photo) restent dans PostgreSQL.

#### 3.2.3 Super-Like (Opération Mixte Critique)

```
Flux d'exécution:
1. @Transactional → Début transaction PostgreSQL
2. SELECT ... FOR UPDATE wallet utilisateur
3. Vérification balance >= coût
4. UPDATE wallet.balance - coût
5. UPDATE system_wallet_profit + (coût × 40%)
6. UPDATE system_wallet_redistribution + (coût × 60%)
7. INSERT transaction d'audit dans PostgreSQL
8. CREATE relation (:User)-[:SUPER_LIKED {date, amount}]->(:Recipe) dans Neo4j
9. UPDATE Recipe.superLikeCount dans Neo4j (compteur de popularité)
10. Si Neo4j échoue → Exception → Rollback PostgreSQL (argent remboursé)
11. Si tout OK → COMMIT PostgreSQL
```

**Justification de la duplication** : La relation [:SUPER_LIKED] existe dans les deux systèmes car :
- PostgreSQL : Trace financière légale et audit
- Neo4j : Calcul rapide de popularité pour le feed

#### 3.2.4 Follow/Unfollow d'un Utilisateur

```
Flux d'exécution:
1. @Transactional → Début transaction PostgreSQL
2. INSERT INTO followers (follower_id, followed_id) dans PostgreSQL
3. CREATE (:User)-[:FOLLOWS]->(:User) dans Neo4j
4. Si Neo4j échoue → Exception → Rollback PostgreSQL
5. Si tout OK → COMMIT PostgreSQL
```

La relation de suivi est dupliquée car :
- PostgreSQL : Source de vérité pour la liste complète des abonnements
- Neo4j : Calcul rapide du feed (traversée du graphe social)

### 3.3 Gestion des Incohérences - Approche Simplifiée

L'équipe adopte une stratégie pragmatique adaptée au contexte académique :

#### 3.3.1 Stratégie de Rollback Automatique

**Principe** : Utiliser le mécanisme transactionnel de Spring pour annuler automatiquement PostgreSQL si Neo4j échoue.

```java
@Service
public class RecipeService {
    
    @Transactional // Transaction PostgreSQL gérée par Spring
    public void publishRecipe(RecipeDTO dto) {
        String recipeId = UUID.randomUUID().toString();
        
        // Étape 1 : Sauvegarde PostgreSQL
        Recipe recipeSql = new Recipe(recipeId, dto);
        recipeSqlRepository.save(recipeSql);
        
        // Étape 2 : Sauvegarde Neo4j
        try {
            RecipeNode recipeNode = new RecipeNode(recipeId, dto.getTitle());
            recipeGraphRepository.save(recipeNode);
        } catch (Exception e) {
            // Si Neo4j échoue, l'exception remonte
            // Spring rollback automatiquement la transaction PostgreSQL
            log.error("Échec sync Neo4j pour recette {}", recipeId, e);
            throw new SyncException("Impossible de synchroniser Neo4j", e);
        }
        
        // Si on arrive ici, tout est OK, COMMIT automatique
    }
}
```

**Mécanisme** : 
- L'annotation `@Transactional` de Spring ouvre une transaction PostgreSQL au début de la méthode
- Si une `RuntimeException` est levée (y compris dans le catch), Spring rollback automatiquement
- Ainsi, si Neo4j échoue, PostgreSQL est annulé, garantissant la cohérence

#### 3.3.2 Log des Incohérences Potentielles

Dans les rares cas où la synchronisation échoue malgré le rollback (crash serveur entre les deux opérations, par exemple), l'équipe implémente un système de logging :

```java
@Service
public class SyncLogService {
    
    public void logFailedSync(String entityType, String entityId, String operation) {
        // Log dans un fichier ou une table dédiée
        log.error("SYNC_FAILURE: {} {} {}", entityType, entityId, operation);
        
        // Optionnel : Insertion dans une table sync_queue
        syncQueueRepository.save(new SyncQueueEntry(entityType, entityId, operation));
    }
}
```

Cette table `sync_queue` peut être consultée périodiquement par un administrateur, mais dans le cadre académique, le simple logging suffit.

#### 3.3.3 Pas de Job de Réconciliation Complexe

**Décision** : L'équipe ne met PAS en place de job de réconciliation nocturne complexe. 

**Justification** :
- Le mécanisme de rollback automatique via `@Transactional` évite 99% des incohérences
- Un job de réconciliation nécessiterait de comparer des milliers d'entités chaque nuit, ajoutant une complexité inutile pour un projet académique
- En cas de problème détecté (par les logs), une intervention manuelle ponctuelle est suffisante

**Alternative minimaliste** : Un endpoint admin pour vérifier manuellement la cohérence d'une entité spécifique si besoin :

```
GET /admin/check-sync/recipe/{recipeId}
→ Vérifie si la recette existe dans PostgreSQL ET Neo4j
→ Retourne un rapport de cohérence
```

### 3.4 Schéma Récapitulatif de l'Architecture de Synchronisation

```
┌─────────────────────────────────────────────────────────────┐
│                     COUCHE SERVICE (Java)                    │
│  - Génération UUID                                           │
│  - @Transactional (PostgreSQL)                               │
│  - Try-catch pour Neo4j avec rollback sur exception          │
└─────────────────────────────────────────────────────────────┘
                    │                        │
                    ▼                        ▼
        ┌─────────────────────┐  ┌─────────────────────┐
        │   PostgreSQL        │  │      Neo4j          │
        │   (SOURCE DE VÉRITÉ)│  │  (VUE OPTIMISÉE)    │
        ├─────────────────────┤  ├─────────────────────┤
        │ Users (complet)     │  │ Users (id, username)│
        │ Recipes (complet)   │  │ Recipes (id, title) │
        │ Wallets             │  │ Relations:          │
        │ Transactions        │  │  - FOLLOWS          │
        │ Followers           │  │  - PUBLISHED        │
        │ Likes               │  │  - IS_VARIANT_OF    │
        │ SuperLikes          │  │  - LIKED            │
        │                     │  │  - SUPER_LIKED      │
        └─────────────────────┘  └─────────────────────┘
             DONNÉES COMPLÈTES      IDs + STRUCTURE GRAPHE
```

## 4. Évaluation de la Complexité de Mise en Œuvre

### 4.1 Coût de Développement

#### 4.1.1 Solution Relationnelle Pure

**Avantages** :

- **Courbe d'apprentissage faible** : L'équipe maîtrise déjà SQL, Hibernate et Spring Data JPA
- **Écosystème mature** : Documentation abondante, Stack Overflow, exemples
- **Debugging simple** : Un seul système de base de données, logs unifiés
- **Testing standard** : Utilisation de H2 ou PostgreSQL embarqué pour les tests unitaires

**Inconvénients** :

- **Requêtes complexes** : Les CTE récursives pour les variantes et les jointures multiples pour le feed nécessitent une expertise SQL avancée
- **Optimisation chronophage** : Beaucoup de temps passé à tuner les index, analyser les plans de requête (EXPLAIN ANALYZE), ajuster les paramètres PostgreSQL
- **Code métier alourdi** : La logique de calcul des différences entre variantes est déportée dans Java, rendant le code moins lisible
- **Risque de performance** : Avec 100 000+ utilisateurs, le feed social peut dépasser les temps de réponse acceptables

#### 4.1.2 Solution Hybride (PostgreSQL + Neo4j)

**Avantages** :

- **Spécialisation** : Chaque système fait ce qu'il fait de mieux (PostgreSQL pour l'argent, Neo4j pour les graphes)
- **Requêtes expressives** : Cypher est très lisible pour les traversées de graphe, réduisant la complexité du code
- **Performance théorique supérieure** : Neo4j est optimisé pour les traversées, évitant les coûts de jointures multiples
- **Architecture professionnelle** : Démontre la maîtrise d'une approche polyglot persistence valorisée dans l'industrie

**Inconvénients** :

- **Courbe d'apprentissage additionnelle** : L'équipe doit apprendre Cypher et Spring Data Neo4j (environ 1 semaine)
- **Complexité opérationnelle** : Deux bases de données à installer, configurer dans Docker Compose
- **Synchronisation à gérer** : Code de sync maître-réplique ajoute une couche de complexité
- **Testing plus élaboré** : Nécessité d'embarquer Neo4j dans les tests (Neo4j Test Harness)
- **Debugging distribué** : En cas de bug, investigation dans deux systèmes

### 4.2 Risques Identifiés et Mitigation

| Risque | Probabilité | Impact | Mitigation |
|--------|-------------|--------|------------|
| **Bugs de synchronisation** | Moyen | Élevé | Tests unitaires rigoureux, rollback automatique |
| **Courbe d'apprentissage Neo4j** | Faible | Moyen | Sprint de formation initial, tutoriels officiels |
| **Performance Neo4j décevante** | Faible | Élevé | Benchmarks précoces sur données réalistes |
| **Complexité opérationnelle** | Moyen | Faible | Docker Compose, scripts d'initialisation |
| **Incohérences données** | Faible | Moyen | Logging, endpoint de vérification admin |

## 5. Synthèse et Recommandation Finale

### 5.1 Tableau Comparatif Récapitulatif

| Critère | Relationnelle Pure | Hybride (PostgreSQL + Neo4j) |
|---------|-------------------|------------------------------|
| **Complexité Feed Social** | O(f₁ × f₂ × log(f₁ × f₂)) + jointures | O(f₁ × f₂) facteur constant faible |
| **Complexité Variantes** | O(h × log n) par recette | O(h) + O(k × log n) |
| **Sécurité Financière** | ✅ Excellence (ACID natif) | ✅ Équivalent (PostgreSQL conservé) |
| **Redistribution Mensuelle** | O(R × h × log n) | O(R × h) + O(A × log n) |
| **Complexité Développement** | ✅ Plus simple (1 techno) | ⚠️ Apprentissage Neo4j |
| **Complexité Synchronisation** | N/A | ⚠️ Code sync + rollback |
| **Scalabilité Utilisateurs** | ⚠️ Dégradation avec volume | ✅ Performance locale (f₁ × f₂) |
| **Lisibilité Code Social** | ⚠️ SQL verbeux | ✅ Cypher élégant |
| **Évolutivité Fonctionnelle** | ⚠️ Difficile (graphes en SQL) | ✅ Facile (graphe natif) |
| **Risque de Performance** | ⚠️ Élevé pour feed avec volume | ✅ Faible |
| **Duplication Données** | N/A | ✅ Minimale (IDs + structure) |

**Légende** : ✅ Excellent | ⚠️ Acceptable avec réserves

### 5.2 Décision de l'Équipe

#### 5.2.1 Contexte de Décision

L'équipe prend en compte les éléments suivants :

1. **Critère d'évaluation explicite** : "Vous serez évalué en partie sur la capacité de votre site à être utilisé par un très grand nombre d'utilisateurs" → La scalabilité est un critère de notation majeur

2. **Autorisation confirmée** : Le professeur a validé l'utilisation de bases de données complémentaires, dont Neo4j

3. **Objectif pédagogique** : Démontrer la maîtrise d'architectures avancées et de patterns industriels

4. **Durée du projet** : 3 mois, permettant d'absorber la courbe d'apprentissage Neo4j

#### 5.2.2 Recommandation : Architecture Hybride avec Duplication Minimale

L'équipe recommande l'adoption de l'architecture hybride pour les raisons suivantes :

**Justifications techniques** :

1. **Réponse au critère de scalabilité** : L'analyse de complexité algorithmique démontre que le feed social est le goulot d'étranglement dans une solution relationnelle pure. La complexité O(f₁ × f₂ × log(f₁ × f₂)) du DISTINCT et des jointures multiples devient prohibitive avec un grand volume d'utilisateurs. Neo4j résout ce problème avec une complexité O(f₁ × f₂) et un facteur constant très faible.

2. **Duplication minimale = maintenance simplifiée** : En ne stockant dans Neo4j que les IDs et quelques métadonnées légères (titre, username), l'équipe minimise le risque de désynchronisation. Les données volumineuses (photos, instructions, profils complets) restent uniquement dans PostgreSQL, source de vérité.

3. **Séparation des responsabilités claire** :
   - PostgreSQL : Argent, authentification, données complètes
   - Neo4j : Relations sociales, hiérarchies, calculs de popularité
   - Cette séparation rend le code plus modulaire et testable

4. **Anticipation de l'évolution** : Les fonctionnalités futures (recommandations, détection de communautés, analyse d'influence) sont naturellement supportées par le graphe. En SQL, elles nécessiteraient des refactorisations majeures.

5. **Avantage compétitif académique** : Dans un contexte où d'autres groupes opteront probablement pour une solution relationnelle standard, l'architecture polyglot persistence démontre une maturité architecturale supérieure.

**Atténuation des risques** :

- **Synchronisation** : Utilisation du pattern UUID + @Transactional avec rollback automatique, éliminant 99% des risques d'incohérence
- **Apprentissage** : Sprint initial dédié à Neo4j (tutoriels, exercices pratiques)
- **Complexité opérationnelle** : Docker Compose avec scripts d'initialisation automatisés
- **Debugging** : Stratégie de logging claire, endpoints admin de vérification

## 6. Conclusion

L'analyse comparative approfondie menée par l'équipe démontre que, bien que PostgreSQL soit la référence absolue pour la gestion transactionnelle et financière, Neo4j apporte une valeur ajoutée déterminante pour les fonctionnalités sociales et hiérarchiques du projet.

**Analyse de complexité algorithmique** : L'étude théorique révèle que :
- Pour le feed social, Neo4j élimine le coût du DISTINCT (O(f₁ × f₂ × log(f₁ × f₂))) et réduit les jointures multiples
- Pour les variantes, Neo4j transforme une complexité O(h × log n) par recette en O(h) avec un facteur constant négligeable
- Pour la redistribution mensuelle, une seule traversée remplace R CTE récursives

**Architecture minimaliste** : En ne dupliquant que les IDs et métadonnées légères dans Neo4j, l'équipe construit une solution robuste qui :
- Minimise les risques de désynchronisation
- Simplifie la maintenance
- Maximise les performances de traversée
- Conserve PostgreSQL comme unique source de vérité pour les données critiques

**Justification du choix** : L'architecture hybride n'est pas une sur-ingénierie, mais une réponse pragmatique aux exigences contradictoires du cahier des charges : rigueur financière (PostgreSQL), performance de traversée (Neo4j), et scalabilité sociale (graphe natif).

Le surcoût en complexité (apprentissage Neo4j, code de synchronisation) est compensé par :
- Les gains théoriques de performance (analyse de complexité)
- La lisibilité du code (Cypher vs SQL complexe)
- La capacité à répondre au critère de scalabilité explicitement évalué
- La démonstration d'une maturité architecturale valorisée académiquement

En adoptant une approche de séparation des responsabilités (separation of concerns) où chaque système fait ce qu'il fait de mieux, avec une duplication minimale des données, l'équipe construit une architecture robuste, performante et évolutive qui valorisera le projet lors de l'évaluation finale.

**Signature technique** : Ce rapport a été rédigé collégialement par l'équipe projet après analyse des contraintes, étude de complexité algorithmique, et validation de la faisabilité dans les délais impartis.