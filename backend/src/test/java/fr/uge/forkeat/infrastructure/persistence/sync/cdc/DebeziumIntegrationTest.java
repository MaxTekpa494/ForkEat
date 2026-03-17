package fr.uge.forkeat.infrastructure.persistence.sync.cdc;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "debezium.enabled=true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DebeziumIntegrationTest extends AbstractIntegrationTest {

    private final EntityManager entityManager;
    private final Neo4jClient neo4jClient;

    @Value("${app.system.earnings.username}")
    private String systemEarningsUsername;

    @Autowired
    public DebeziumIntegrationTest(EntityManager entityManager, Neo4jClient neo4jClient) {
        this.entityManager = entityManager;
        this.neo4jClient = neo4jClient;
    }

    @AfterAll
    void cleanDatabases() throws Exception {
        // Nettoyage Neo4j
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();

        // Nettoyage PostgreSQL via psql dans le conteneur
        postgres.execInContainer("psql",
                "-U", postgres.getUsername(),
                "-d", postgres.getDatabaseName(),
                "-c", "TRUNCATE TABLE recipe_allergens, recipe_ingredients, recipes, platform_wallets, wallets, user_systems, \"users\" CASCADE");
    }

    @Nested
    class UserTests {
        /**
         * Vérifie qu'un utilisateur créé dans PostgreSQL est bien répliqué dans Neo4j.
         */
        @Test
        @Transactional
        void shouldSyncUserToNeo4jOnCreation() {
            var username = "cdc_user_" + System.currentTimeMillis();
            var user = createUser(username);
            entityManager.persist(user);
            entityManager.flush();
            TestTransaction.flagForCommit();
            TestTransaction.end();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var exists = checkUserExists(username);
                assertThat(exists).as("User should exist in Neo4j").isTrue();
            });
        }

        /**
         * Vérifie qu'une mise à jour (ex: email) dans PostgreSQL est propagée dans Neo4j.
         */
        @Test
        @Transactional
        void shouldSyncUserUpdateToNeo4j() {
            var username = "cdc_user_update_" + System.currentTimeMillis();
            var user = createUser(username);
            entityManager.persist(user);
            entityManager.flush();
            TestTransaction.flagForCommit();
            TestTransaction.end();

            TestTransaction.start();
            var userToUpdate = entityManager.find(UserEntity.class, user.getId());
            var newEmail = "updated_" + username + "@test.com";
            userToUpdate.setEmail(newEmail);
            entityManager.flush();
            TestTransaction.flagForCommit();
            TestTransaction.end();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var email = neo4jClient.query("MATCH (u:User {username: $username}) RETURN u.email")
                        .bind(username).to("username")
                        .fetchAs(String.class).one().orElse(null);
                assertThat(email).isEqualTo(newEmail);
            });
        }

        /**
         * Vérifie que la suppression d'un utilisateur dans PostgreSQL entraîne sa suppression dans Neo4j.
         */
        @Test
        @Transactional
        void shouldSyncUserDeletionToNeo4j() {
            var username = "cdc_user_delete_" + System.currentTimeMillis();
            var user = createUser(username);
            entityManager.persist(user);
            entityManager.flush();
            TestTransaction.flagForCommit();
            TestTransaction.end();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(checkUserExists(username)).isTrue());

            TestTransaction.start();
            var userToDelete = entityManager.find(UserEntity.class, user.getId());
            entityManager.remove(userToDelete);
            entityManager.flush();
            TestTransaction.flagForCommit();
            TestTransaction.end();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var exists = checkUserExists(username);
                assertThat(exists).as("User should be deleted from Neo4j").isFalse();
            });
        }

        /**
         * Vérifie que lorsqu'un utilisateur est supprimé, ses recettes dans Neo4j sont réassignées à 'system_earnings'.
         */
        @Test
        @Transactional
        void shouldReassignRecipesToSystemEarningsOnUserDeletion() {
            var username = "cdc_user_reassign_" + System.currentTimeMillis();
            var user = createUser(username);
            entityManager.persist(user);
            entityManager.flush();
            TestTransaction.flagForCommit();
            TestTransaction.end();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(checkUserExists(username)).isTrue());

            var dummyRecipeId = UUID.randomUUID().toString();
            neo4jClient.query("MATCH (u:User {username: $username}) CREATE (r:Recipe {id: $id, title: 'Dummy'})<-[:PUBLISHED]-(u)")
                    .bind(dummyRecipeId).to("id")
                    .bind(username).to("username")
                    .run();

            TestTransaction.start();
            var userToDelete = entityManager.find(UserEntity.class, user.getId());
            entityManager.remove(userToDelete);
            entityManager.flush();
            TestTransaction.flagForCommit();
            TestTransaction.end();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var reassigned = neo4jClient.query("""
                        MATCH (u:User {username: $systemUsername})-[:PUBLISHED]->(r:Recipe {id: $id})
                        RETURN count(r) > 0
                        """)
                        .bind(dummyRecipeId).to("id")
                        .bind(systemEarningsUsername).to("systemUsername")
                        .fetchAs(Boolean.class).one().orElse(false);
                assertThat(reassigned).as("Recipe should be reassigned to system_earnings").isTrue();
            });
        }

        /**
         * Vérifie qu'un utilisateur ayant des relations SUPER_LIKED n'est pas supprimé physiquement mais marqué deleted.
         */
        @Test
        @Transactional
        void shouldMarkUserAsDeletedWhenHavingSuperLikes() {
            var username = "cdc_user_super_" + System.currentTimeMillis();
            var user = createUser(username);
            entityManager.persist(user);
            entityManager.flush();
            TestTransaction.flagForCommit();
            TestTransaction.end();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(checkUserExists(username)).isTrue());

            neo4jClient.query("""
                    MATCH (u:User {username: $username})
                    CREATE (u)-[:SUPER_LIKED {amount: 100}]->(:Recipe {id: 'some-recipe'})
                    """)
                    .bind(username).to("username")
                    .run();

            TestTransaction.start();
            var userToDelete = entityManager.find(UserEntity.class, user.getId());
            entityManager.remove(userToDelete);
            entityManager.flush();
            TestTransaction.flagForCommit();
            TestTransaction.end();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var isDeleted = neo4jClient.query("MATCH (u:User {username: $username}) RETURN u.deleted")
                        .bind(username).to("username")
                        .fetchAs(Boolean.class).one().orElse(false);
                assertThat(isDeleted).as("User should be marked as deleted").isTrue();
            });
        }
    }

    @Nested
    class RecipeTests {
        /**
         * Vérifie la création d'une recette et sa liaison PUBLISHED avec l'auteur.
         */
        @Test
        @Transactional
        void shouldSyncRecipeCreationAndLinkToAuthor() {
            var username = "cdc_author_" + System.currentTimeMillis();
            var author = createUser(username);
            entityManager.persist(author);
            var recipe = createRecipe("CDC Recipe", author);
            entityManager.persist(recipe);
            entityManager.flush();

            TestTransaction.flagForCommit();
            TestTransaction.end();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                assertThat(checkRecipeExists(recipe.getTitle())).isTrue();
                assertThat(checkRecipeLinkedToUser(recipe.getTitle(), username)).isTrue();
            });
        }

        /**
         * Vérifie que la relation IS_VARIANT_OF est créée lorsqu'une recette a un parent.
         */
        @Test
        @Transactional
        void shouldCreateRecipeVariantRelationship() {
            var username = "cdc_author_variant_" + System.currentTimeMillis();
            var author = createUser(username);
            entityManager.persist(author);
            var parentRecipe = createRecipe("Parent Recipe", author);
            entityManager.persist(parentRecipe);
            entityManager.flush();
            var variantRecipe = createRecipe("Variant Recipe", author);
            variantRecipe.setParent(parentRecipe);
            entityManager.persist(variantRecipe);
            entityManager.flush();

            TestTransaction.flagForCommit();
            TestTransaction.end();

            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                var isVariant = neo4jClient.query("""
                        MATCH (v:Recipe {title: $vTitle})-[:IS_VARIANT_OF]->(p:Recipe {title: $pTitle})
                        RETURN count(v) > 0
                        """)
                        .bind(variantRecipe.getTitle()).to("vTitle")
                        .bind(parentRecipe.getTitle()).to("pTitle")
                        .fetchAs(Boolean.class).one().orElse(false);
                assertThat(isVariant).as("Variant relationship should exist").isTrue();
            });
        }

        /**
         * Vérifie qu'une recette avec des SUPER_LIKED n'est pas supprimée mais marquée 'deleted' et réassignée au système.
         */
        @Test
        @Transactional
        void shouldHandleRecipeDeletionWithSuperLikes() {
            var username = "cdc_author_sl_" + System.currentTimeMillis();
            var author = createUser(username);
            entityManager.persist(author);
            var recipe = createRecipe("Super Liked Recipe", author);
            entityManager.persist(recipe);
            entityManager.flush();
            TestTransaction.flagForCommit();
            TestTransaction.end();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(checkRecipeExists(recipe.getTitle())).isTrue());
            
            neo4jClient.query("""
                    MATCH (r:Recipe {title: $title})
                    CREATE (:User {username: 'fan'})-[:SUPER_LIKED]->(r)
                    """)
                    .bind(recipe.getTitle()).to("title")
                    .run();

            TestTransaction.start();
            var recipeToDelete = entityManager.find(RecipeEntity.class, recipe.getId());
            entityManager.remove(recipeToDelete);
            entityManager.flush();
            TestTransaction.flagForCommit();
            TestTransaction.end();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var isDeleted = neo4jClient.query("MATCH (r:Recipe {title: $title}) RETURN r.deleted_at IS NOT NULL")
                        .bind(recipe.getTitle()).to("title")
                        .fetchAs(Boolean.class).one().orElse(false);
                assertThat(isDeleted).as("Recipe should be marked deleted").isTrue();

                var ownedBySystem = neo4jClient.query("""
                        MATCH (u:User {username: $systemUsername})-[:PUBLISHED]->(r:Recipe {title: $title})
                        RETURN count(r) > 0
                        """)
                        .bind(recipe.getTitle()).to("title")
                        .bind(systemEarningsUsername).to("systemUsername")
                        .fetchAs(Boolean.class).one().orElse(false);
                assertThat(ownedBySystem).as("Recipe should be reassigned to system").isTrue();
            });
        }

        /**
         * Vérifie que lors de la suppression d'une recette parente, l'enfant est préservé (dans Neo4j).
         */
        @Test
        @Transactional
        void shouldRepairVariantTreeOnRecipeDeletion() {
            var username = "cdc_author_tree_" + System.currentTimeMillis();
            var author = createUser(username);
            entityManager.persist(author);
            var parent = createRecipe("Parent", author);
            entityManager.persist(parent);
            var child = createRecipe("Child", author);
            child.setParent(parent);
            entityManager.persist(child);
            
            entityManager.flush();
            TestTransaction.flagForCommit();
            TestTransaction.end();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var childLinked = neo4jClient.query("""
                        MATCH (c:Recipe {title: 'Child'})-[:IS_VARIANT_OF]->(p:Recipe {title: 'Parent'})
                        RETURN count(c) > 0
                        """).fetchAs(Boolean.class).one().orElse(false);
                assertThat(childLinked).isTrue();
            });

            TestTransaction.start();
            var childEntity = entityManager.find(RecipeEntity.class, child.getId());
            childEntity.setParent(null);
            entityManager.persist(childEntity);
            var parentEntity = entityManager.find(RecipeEntity.class, parent.getId());
            entityManager.remove(parentEntity);
            entityManager.flush();
            TestTransaction.flagForCommit();
            TestTransaction.end();

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var parentExists = checkRecipeExists("Parent");
                assertThat(parentExists).isFalse();
                var childExists = checkRecipeExists("Child");
                assertThat(childExists).isTrue();
            });
        }
    }

    private UserEntity createUser(String username) {
        var user = new UserEntity();
        user.setUsername(username);
        user.setFirstName("CDC");
        user.setLastName("Test");
        user.setEmail(username + "@test.com");
        user.setPassword("pass");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.LOCAL);
        
        var wallet = new WalletEntity();
        wallet.setId(UUID.randomUUID());
        wallet.setBalance(0L);
        user.setWallet(wallet);
        return user;
    }

    private RecipeEntity createRecipe(String title, UserEntity author) {
        var recipe = new RecipeEntity();
        recipe.setTitle(title);
        recipe.setSummary("Summary for " + title);
        recipe.setAuthor(author);
        recipe.setStatus(RecipeStatus.PUBLISHED);
        recipe.setStepByStepInstructions(new ArrayList<>());
        return recipe;
    }

    private boolean checkUserExists(String username) {
        return neo4jClient.query("MATCH (u:User {username: $username}) RETURN count(u) > 0")
                .bind(username).to("username")
                .fetchAs(Boolean.class).one().orElse(false);
    }

    private boolean checkRecipeExists(String title) {
        return neo4jClient.query("MATCH (r:Recipe {title: $title}) RETURN count(r) > 0")
                .bind(title).to("title")
                .fetchAs(Boolean.class).one().orElse(false);
    }

    private boolean checkRecipeLinkedToUser(String recipeTitle, String username) {
        return neo4jClient.query("""
                MATCH (u:User {username: $username})-[:PUBLISHED]->(r:Recipe {title: $title})
                RETURN count(r) > 0
                """)
                .bind(username).to("username")
                .bind(recipeTitle).to("title")
                .fetchAs(Boolean.class).one().orElse(false);
    }
}
