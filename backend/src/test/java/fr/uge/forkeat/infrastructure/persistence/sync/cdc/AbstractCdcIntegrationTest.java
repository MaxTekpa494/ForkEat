package fr.uge.forkeat.infrastructure.persistence.sync.cdc;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public abstract class AbstractCdcIntegrationTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCdcIntegrationTest.class);
  private static final Network network = Network.newNetwork();

  static final PostgreSQLContainer<?> configPostgres = new PostgreSQLContainer<>("postgres:15")
          .withNetwork(network)
          .withNetworkAliases("postgres")
          .withUsername("postgres")
          .withPassword("postgres")
          .withDatabaseName("postgres")
          .withCommand("postgres -c wal_level=logical -c max_connections=200");

  static final ConfluentKafkaContainer configKafka = new ConfluentKafkaContainer(DockerImageName
          .parse("confluentinc/cp-kafka:7.7.0")
          .asCompatibleSubstituteFor("apache/kafka"))
          .withNetwork(network)
          .withNetworkAliases("kafka")
          .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
          .withLogConsumer(new Slf4jLogConsumer(LOGGER));

  // Neo4j n'est plus hérité du parent : on le déclare ici.
  // Pas besoin du réseau Docker, seul le Spring de test (hors réseau) s'y connecte.
  static final Neo4jContainer<?> configNeo4j = new Neo4jContainer<>(DockerImageName.parse("neo4j:5"))
          .withAdminPassword("password")
          .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
          .withLogConsumer(new Slf4jLogConsumer(LOGGER));

  static final GenericContainer<?> configDebezium = new GenericContainer<>("quay.io/debezium/server:3.6.3.Final")
          .withNetwork(network)
          .withNetworkAliases("debezium")
          .dependsOn(configPostgres, configKafka)
          .withExposedPorts(8080)

          // ── Sink : Kafka ──────────────────────────────────────────────
          .withEnv("DEBEZIUM_SINK_TYPE", "kafka")
          .withEnv("DEBEZIUM_SINK_KAFKA_PRODUCER_BOOTSTRAP_SERVERS", "kafka:9093")
          .withEnv("DEBEZIUM_SINK_KAFKA_PRODUCER_ACKS", "all")
          .withEnv("DEBEZIUM_SINK_KAFKA_PRODUCER_RETRIES", "3")
          .withEnv("DEBEZIUM_SINK_KAFKA_PRODUCER_ENABLE_IDEMPOTENCE", "true")
          .withEnv("DEBEZIUM_SINK_KAFKA_PRODUCER_KEY_SERIALIZER",
                  "org.apache.kafka.common.serialization.StringSerializer")
          .withEnv("DEBEZIUM_SINK_KAFKA_PRODUCER_VALUE_SERIALIZER",
                  "org.apache.kafka.common.serialization.StringSerializer")

          // ── Source : PostgreSQL ───────────────────────────────────────
          .withEnv("DEBEZIUM_SOURCE_CONNECTOR_CLASS",
                  "io.debezium.connector.postgresql.PostgresConnector")
          .withEnv("DEBEZIUM_SOURCE_OFFSET_STORAGE_FILE_FILENAME", "/debezium/data/offsets.json")
          .withEnv("DEBEZIUM_SOURCE_OFFSET_FLUSH_INTERVAL_MS", "1000")
          .withEnv("DEBEZIUM_SOURCE_DATABASE_HOSTNAME", "postgres")
          .withEnv("DEBEZIUM_SOURCE_DATABASE_PORT", "5432")
          .withEnv("DEBEZIUM_SOURCE_DATABASE_USER", "postgres")
          .withEnv("DEBEZIUM_SOURCE_DATABASE_PASSWORD", "postgres")
          .withEnv("DEBEZIUM_SOURCE_DATABASE_DBNAME", "postgres")
          .withEnv("DEBEZIUM_SOURCE_TOPIC_PREFIX", "forkeat")
          .withEnv("DEBEZIUM_SOURCE_TABLE_INCLUDE_LIST",
                  "public.users,public.recipes,public.super_likes")
          .withEnv("DEBEZIUM_SOURCE_COLUMN_EXCLUDE_LIST", "public.users.password")
          .withEnv("DEBEZIUM_SOURCE_PLUGIN_NAME", "pgoutput")
          .withEnv("DEBEZIUM_SOURCE_PUBLICATION_AUTOCREATE_MODE", "filtered")
          .withEnv("DEBEZIUM_SOURCE_SLOT_NAME", "forkeat_slot")
          .withEnv("DEBEZIUM_SOURCE_SNAPSHOT_MODE", "initial")
          .withEnv("DEBEZIUM_SOURCE_TOMBSTONES_ON_DELETE", "false")

          // ── Format ────────────────────────────────────────────────────
          .withEnv("DEBEZIUM_FORMAT_KEY", "json")
          .withEnv("DEBEZIUM_FORMAT_VALUE", "json")
          .withEnv("DEBEZIUM_FORMAT_KEY_SCHEMAS_ENABLE", "false")
          .withEnv("DEBEZIUM_FORMAT_VALUE_SCHEMAS_ENABLE", "false")

          // ── Readiness ─────────────────────────────────────────────────
          .withLogConsumer(new Slf4jLogConsumer(LOGGER))
          .waitingFor(Wait.forLogMessage(".*Processing messages.*", 1));
  //.waitingFor(Wait.forHttp("/q/health/ready").forPort(8080));


  static {
    configKafka.start();
    configPostgres.start();
    configNeo4j.start();

    Flyway.configure()
            .dataSource(configPostgres.getJdbcUrl(),
                    configPostgres.getUsername(),
                    configPostgres.getPassword())
            .locations("classpath:db/testmigration")
            .placeholders(Map.of("system_earnings_password_hash", "un-hash-bidon-pour-les-tests"))
            .load()
            .migrate();

    // Debezium démarre en dernier : les tables existent pour la publication "filtered"
    configDebezium.start();
  }

  @DynamicPropertySource
  static void configureCdcProperties(DynamicPropertyRegistry registry) {
    // PostgreSQL
    registry.add("spring.datasource.url", () -> configPostgres.getJdbcUrl() + "&stringtype=unspecified");
    registry.add("spring.datasource.username", configPostgres::getUsername);
    registry.add("spring.datasource.password", configPostgres::getPassword);

    // Kafka (adresse visible depuis la JVM de test, hors réseau Docker)
    registry.add("spring.kafka.bootstrap-servers", configKafka::getBootstrapServers);
    registry.add("spring.kafka.consumer.properties.metadata.max.age.ms", () -> "1000");

    // Neo4j (repris de AbstractIntegrationTest)
    registry.add("spring.neo4j.uri", configNeo4j::getBoltUrl);
    registry.add("spring.neo4j.authentication.username", () -> "neo4j");
    registry.add("spring.neo4j.authentication.password", configNeo4j::getAdminPassword);
  }
}