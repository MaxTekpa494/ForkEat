package fr.uge.forkeat.infrastructure.persistence;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractIntegrationTest {

    // Déclaration statique = Singleton
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("forkeat_test")
            .withUsername("test")
            .withPassword("test")
            // la réplication logique pour debezium
            .withCommand("postgres", "-c", "wal_level=logical");

    static final Neo4jContainer<?> neo4j = new Neo4jContainer<>(DockerImageName.parse("neo4j:5"))
            .withAdminPassword("password");

    static {
        postgres.start();
        neo4j.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Config Spring Datasource
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "&stringtype=unspecified");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Config Debezium (pour qu'il se connecte au même conteneur)
        registry.add("debezium.database.hostname", postgres::getHost);
        registry.add("debezium.database.port", postgres::getFirstMappedPort);
        registry.add("debezium.database.user", postgres::getUsername);
        registry.add("debezium.database.password", postgres::getPassword);
        registry.add("debezium.database.dbname", postgres::getDatabaseName);

        // Config Neo4j
        registry.add("spring.neo4j.uri", neo4j::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", neo4j::getAdminPassword);
    }
}
