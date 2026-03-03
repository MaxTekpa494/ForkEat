package fr.uge.forkeat.infrastructure.config;

import fr.uge.forkeat.infrastructure.persistence.sync.cdc.DebeziumCDCListener;
import io.debezium.config.Configuration;
import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Configuration du moteur Debezium Embedded pour la capture des changements de données (CDC).
 * Debezium surveille les modifications dans PostgreSQL et les transmet au listener pour synchronisation avec Neo4j.
 */
@Component
@ConditionalOnProperty(name = "debezium.enabled", havingValue = "true")
public class DebeziumConfig {

    private static final Logger log = LoggerFactory.getLogger(DebeziumConfig.class);

    private final DebeziumCDCListener cdcListener;

    // Chemin du fichier stockant les offsets (position de lecture dans le WAL PostgreSQL)
    @Value("${debezium.offset-storage-file}")
    private String offsetStorageFile;

    // Paramètres de connexion à PostgreSQL
    @Value("${debezium.database.hostname}")
    private String dbHostname;

    @Value("${debezium.database.port}")
    private String dbPort;

    @Value("${debezium.database.user}")
    private String dbUser;

    @Value("${debezium.database.password}")
    private String dbPassword;

    @Value("${debezium.database.dbname}")
    private String dbName;

    // Moteur Debezium et son exécuteur
    private DebeziumEngine<RecordChangeEvent<SourceRecord>> engine;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public DebeziumConfig(DebeziumCDCListener cdcListener) {
        this.cdcListener = cdcListener;
    }

    /**
     * Démarre le moteur Debezium au lancement de l'application.
     */
    @PostConstruct
    public void start() {
        log.info("Démarrage de Debezium Embedded Engine...");

        Configuration config = Configuration.create()
                // Nom unique du connecteur
                .with("name", "forkeat-postgres-connector")
                // Type de connecteur : PostgreSQL
                .with("connector.class", "io.debezium.connector.postgresql.PostgresConnector")
                // Stockage des offsets dans un fichier local (position de lecture)
                .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                .with("offset.storage.file.filename", offsetStorageFile)
                .with("offset.flush.interval.ms", "1000")
                // Configuration de connexion PostgreSQL
                .with("database.hostname", dbHostname)
                .with("database.port", dbPort)
                .with("database.user", dbUser)
                .with("database.password", dbPassword)
                .with("database.dbname", dbName)
                .with("database.server.name", "forkeat")
                .with("topic.prefix", "forkeat")
                // Liste des tables à surveiller pour les changements
                .with("table.include.list", "public.users,public.recipes,public.super_likes")
                // Plugin de décodage PostgreSQL (pgoutput est le standard)
                .with("plugin.name", "pgoutput")
                // Création automatique de la publication PostgreSQL
                .with("publication.autocreate.mode", "filtered")
                // Nom du slot de réplication PostgreSQL
                .with("slot.name", "forkeat_slot")
                // Stockage de l'historique du schéma en mémoire (simplifié pour le dev)
                .with("schema.history.internal", "io.debezium.relational.history.MemorySchemaHistory")
                // Mode snapshot : capture l'état initial des tables au premier démarrage
                .with("snapshot.mode", "initial")
                .build();

        // Création et démarrage du moteur Debezium
        this.engine = DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
                .using(config.asProperties())
                .notifying(cdcListener::handleChangeEvent)  // Callback appelé à chaque changement
                .build();

        executor.execute(engine);
        log.info("Debezium Embedded Engine démarré avec succès");
    }

    /**
     * Arrête proprement le moteur Debezium à l'arrêt de l'application.
     */
    @PreDestroy
    public void stop() throws IOException, InterruptedException {
        if (engine != null) {
            log.info("Arrêt de Debezium Embedded Engine...");
            engine.close();
        }
        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            log.warn("Debezium ne s'est pas arrêté proprement dans le délai imparti");
            executor.shutdownNow();
        }
    }
}
