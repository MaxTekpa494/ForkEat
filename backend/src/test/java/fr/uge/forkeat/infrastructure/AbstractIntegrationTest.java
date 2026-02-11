package fr.uge.forkeat.infrastructure;

import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class AbstractIntegrationTest {

    private static final String OFFSET_FILE_PATH = "debezium-offsets-test.dat";

    // Déclaration statique = Singleton
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
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
        // Ajout de la propriété manquante pour le fichier de stockage des offsets
        registry.add("debezium.offset-storage-file", () -> OFFSET_FILE_PATH);

        // Config Neo4j
        registry.add("spring.neo4j.uri", neo4j::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", neo4j::getAdminPassword);
    }

    @AfterAll
    static void cleanup() {
        try {
            Files.deleteIfExists(Paths.get(OFFSET_FILE_PATH));
        } catch (IOException e) {
            // Ignorer si le fichier ne peut pas être supprimé, ce n'est pas critique pour le test suivant
            // mais on peut le logger si nécessaire
            System.err.println("Failed to delete Debezium offset file: " + e.getMessage());
        }
    }
}
