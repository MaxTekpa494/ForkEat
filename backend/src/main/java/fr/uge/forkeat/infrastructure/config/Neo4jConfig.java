package fr.uge.forkeat.infrastructure.config;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableNeo4jRepositories(
        basePackages = "fr.uge.forkeat.infrastructure.persistence.neo4j",
        transactionManagerRef = "neo4jTransactionManager"
)
public class Neo4jConfig {

    private final Logger log = LoggerFactory.getLogger(Neo4jConfig.class);
    @Bean("neo4jTransactionManager")
    public PlatformTransactionManager neo4jTransactionManager(Driver driver) {
        return new Neo4jTransactionManager(driver);
    }

    /**
     * Crée les contraintes d'unicité Neo4j au démarrage.
     * IF NOT EXISTS rend l'opération idempotente : aucun risque à chaque redémarrage.
     * ApplicationRunner s'exécute après le démarrage complet du contexte Spring,
     * garantissant que Neo4j est joignable.
     */
    @Bean
    public ApplicationRunner neo4jConstraintsInitializer(Driver driver) {
        return (ApplicationArguments _) -> {
            log.info("Vérification des contraintes Neo4j...");

            try (Session session = driver.session()) {
                session.run("""
                    CREATE CONSTRAINT recipe_id_unique IF NOT EXISTS
                    FOR (r:Recipe) REQUIRE r.id IS UNIQUE
                    """);

                session.run("""
                    CREATE CONSTRAINT user_id_unique IF NOT EXISTS
                    FOR (u:User) REQUIRE u.id IS UNIQUE
                    """);
            }

            log.info("Contraintes Neo4j vérifiées");
        };
    }
}