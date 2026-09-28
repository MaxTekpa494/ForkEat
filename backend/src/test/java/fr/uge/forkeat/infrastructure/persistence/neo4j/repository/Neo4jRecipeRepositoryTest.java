package fr.uge.forkeat.infrastructure.persistence.neo4j.repository;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.infrastructure.persistence.neo4j.node.RecipeNode;
import fr.uge.forkeat.infrastructure.persistence.neo4j.node.UserNode;
import org.junit.Before;
import org.junit.jupiter.api.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.neo4j.test.autoconfigure.DataNeo4jTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataNeo4jTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class Neo4jRecipeRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private Neo4jRecipeRepository recipeRepository;

    @Autowired
    private Neo4jUserRepository userRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private Driver driver;

    private UUID userId1;
    private UUID userId2;
    private UUID userId3;
    private UUID recipeId1;
    private UUID recipeId2;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        userId3 = UUID.randomUUID();
        recipeId1 = UUID.randomUUID();
        recipeId2 = UUID.randomUUID();
    }

    private void cleanDatabase() {
        // Nettoyage via une session brute (comme les écritures faites ailleurs dans cette classe
        // via driver.session()), car ces écritures commitent réellement et échappent donc
        // au rollback du @Transactional de la classe : deleteAll() seul (géré par Spring) est
        // lui-même annulé en fin de test et ne supprime jamais ces nœuds pour de vrai.
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n");
        }
    }

    @Test
    @DisplayName("Should create a new LIKED relationship and return true")
    void shouldCreateNewLike() {
        createUser(userId1, "user1");
        createRecipe(recipeId1);

        recipeRepository.likeRecipe(userId1, recipeId1);

        assertTrue(hasLiked(userId1, recipeId1), "Like relationship should exist");
    }

    @Test
    @DisplayName("Multiple users can like the same recipe")
    void multipleUsersCanLikeSameRecipe() {
        createUser(userId1, "user1");
        createUser(userId2, "user2");
        createRecipe(recipeId1);
        // When
        recipeRepository.likeRecipe(userId1, recipeId1);
        recipeRepository.likeRecipe(userId2, recipeId1);

        // Then
        long likeCount = recipeRepository.nbLike(recipeId1);
        assertThat(likeCount).isEqualTo(2L);
    }

    @Test
    @DisplayName("A user can like multiple recipes")
    void userCanLikeMultipleRecipes() {
        // Given
        createUser(userId1, "user1");
        createRecipe(recipeId1);
        createRecipe(recipeId2);

        // When
        recipeRepository.likeRecipe(userId1, recipeId1);
        recipeRepository.likeRecipe(userId1, recipeId2);

        // Then
        long userLikeCount = countLikes(userId1, recipeId1);
        long userLikeCount2 = countLikes(userId1, recipeId2);
        assertThat(userLikeCount).isEqualTo(1L);
        assertThat(userLikeCount2).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should delete relation")
    void shouldDeleteExistingLikedRelationship() {
        createUser(userId1, "user1");
        createRecipe(recipeId1);
        // Given
        recipeRepository.likeRecipe(userId1, recipeId1);
        assertThat(hasLiked(userId1, recipeId1)).isTrue();

        // When
        recipeRepository.unlikeRecipe(userId1, recipeId1);

        // Then
        boolean hasLiked = hasLiked(userId1, recipeId1);
        assertThat(hasLiked).isFalse();
    }

    @Test
    @DisplayName("Should be idempotent , should not throw error")
    void shouldBeIdempotent() {
        createUser(userId1, "user1");
        createRecipe(recipeId1);

        // Given
        recipeRepository.likeRecipe(userId1, recipeId1);

        // When/Then - ne devrait pas lancer d'exception
        Assertions.assertDoesNotThrow(() -> {
            recipeRepository.unlikeRecipe(userId1, recipeId1);
            recipeRepository.unlikeRecipe(userId1, recipeId1);
        });

        boolean hasLiked = hasLiked(userId1, recipeId1);
        assertThat(hasLiked).isFalse();
    }

    @Test
    @DisplayName("Should not affect other relations")
    void shouldNotAffectOtherRelationships() {
        // Given
        createUser(userId1, "user1");
        createUser(userId2, "user2");
        createRecipe(recipeId1);

        recipeRepository.likeRecipe(userId1, recipeId1);
        recipeRepository.likeRecipe(userId2, recipeId1);

        // When
        recipeRepository.unlikeRecipe(userId1, recipeId1);

        // Then
        assertThat(hasLiked(userId1, recipeId1)).isFalse();
        assertThat(hasLiked(userId2, recipeId1)).isTrue();
        assertThat(recipeRepository.nbLike(recipeId1)).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should not remove recipe or user")
    void shouldNotDeleteUserOrRecipe() {
        createUser(userId1, "user1");
        createRecipe(recipeId1);

        // Given
        recipeRepository.likeRecipe(userId1, recipeId1);

        // When
        recipeRepository.unlikeRecipe(userId1, recipeId1);

        // Then
        assertThat(recipeRepository.findById(recipeId1)).isPresent();
    }

    @Test
    @DisplayName("Should return false when like already exists")
    void shouldNotCreateDuplicateLike() {
        createUser(userId1, "user1");
        createRecipe(recipeId1);

        recipeRepository.likeRecipe(userId1, recipeId1);
        recipeRepository.likeRecipe(userId1, recipeId1);

        assertEquals(1, countLikes(userId1, recipeId1), "Should have exactly one like");
    }

    @Test
    @DisplayName("hasLiked should return false when no like exists")
    void hasLikedShouldReturnFalseWhenNoLike() {
        createUser(userId1, "user1");
        createRecipe(recipeId1);

        assertFalse(hasLiked(userId1, recipeId1),
                "Should return false when no like exists");
    }

    @Test
    @DisplayName("hasLiked should return true after liking")
    void hasLikedShouldReturnTrueAfterLiking() {
        createUser(userId1, "user1");
        createRecipe(recipeId1);

        recipeRepository.likeRecipe(userId1, recipeId1);

        assertTrue(hasLiked(userId1, recipeId1),
                "Should return true after liking");
    }

    @Test
    @DisplayName("Should handle concurrent like attempts without duplicates")
    void shouldHandleConcurrentLikes() throws InterruptedException {
        try (var session = driver.session()) {
            session.run("MERGE (u:User {id: $uuid}) SET u.username = $username",
                    java.util.Map.of("uuid", userId1.toString(), "username", "user1"));
            session.run("MERGE (r:Recipe {id: $uuid})",
                    java.util.Map.of("uuid", recipeId1.toString()));
        }

        int threadCount = 10;
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        recipeRepository.likeRecipe(userId1, recipeId1);
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        // Verification
        long count = 0;
        try (Session session = driver.session()) {
             count = session.run("""        
                    MATCH (r:Recipe {id: $recipeId})
                    MATCH (u:User {id: $userId})
                    OPTIONAL MATCH (r)<-[l:LIKED]-(u)
                    RETURN count(l) AS nbLike
            """,
                    java.util.Map.of("userId", userId1.toString(), "recipeId", recipeId1.toString())
            ).single().get("nbLike").asLong();
        }

        assertEquals(1, count, "Should have exactly one like in database");
    }

    @Test
    @DisplayName("Should allow different users to like the same recipe")
    void shouldAllowMultipleUsersToLikeSameRecipe() {
        createUser(userId1, "user1");
        createUser(userId2, "user2");
        createRecipe(recipeId1);

        recipeRepository.likeRecipe(userId1, recipeId1);
        recipeRepository.likeRecipe(userId2, recipeId1);

        assertTrue(hasLiked(userId1, recipeId1),
                "User1 like should exist");
        assertTrue(hasLiked(userId2, recipeId1),
                "User2 like should exist");
    }

    @Test
    @DisplayName("Should allow same user to like different recipes")
    void shouldAllowUserToLikeMultipleRecipes() {
        createUser(userId1, "user1");
        createRecipe(recipeId1);
        createRecipe(recipeId2);

        recipeRepository.likeRecipe(userId1, recipeId1);
        recipeRepository.likeRecipe(userId1, recipeId2);

        assertTrue(hasLiked(userId1, recipeId1),
                "Like for recipe1 should exist");
        assertTrue(hasLiked(userId1, recipeId2),
                "Like for recipe2 should exist");
    }

    @Test
    @DisplayName("Should not create duplicate likes for same user and recipe")
    void shouldNotCreateDuplicateLikesMultipleTimes() {
        createUser(userId1, "user1");
        createRecipe(recipeId1);

        recipeRepository.likeRecipe(userId1, recipeId1);
        recipeRepository.likeRecipe(userId1, recipeId1);
        recipeRepository.likeRecipe(userId1, recipeId1);

        assertEquals(1, countLikes(userId1, recipeId1),
                "Should have exactly one like");
    }

    @Test
    @DisplayName("Should handle rapid successive likes correctly")
    void shouldHandleRapidSuccessiveLikes() throws InterruptedException {
        createUser(userId1, "user1");
        createRecipe(recipeId1);

        recipeRepository.likeRecipe(userId1, recipeId1);
        Thread.sleep(1); // Minimal delay
        recipeRepository.likeRecipe(userId1, recipeId1);
        Thread.sleep(1);
        recipeRepository.likeRecipe(userId1, recipeId1);

        assertEquals(1, countLikes(userId1, recipeId1));
    }

    @Test
    @DisplayName("findById should work correctly")
    void shouldFindRecipeById() {
        createRecipe(recipeId1);

        var recipe = recipeRepository.findById(recipeId1);

        assertTrue(recipe.isPresent(), "Recipe should be found");
        assertEquals(recipeId1, recipe.get().getId(), "UUID should match");
    }

    @Test
    @DisplayName("findById should return empty for non-existent recipe")
    void shouldReturnEmptyForNonExistentRecipe() {
        UUID nonExistentId = UUID.randomUUID();

        var recipe = recipeRepository.findById(nonExistentId);

        assertFalse(recipe.isPresent(), "Should return empty for non-existent recipe");
    }

    @Test
    @DisplayName("Scénario complet: Like -> Unlike -> Like")
    void completeLikeUnlikeLikeScenario() {
        createUser(userId1, "user1");
        createRecipe(recipeId1);

        // Like
        recipeRepository.likeRecipe(userId1, recipeId1);
        assertThat(hasLiked(userId1, recipeId1)).isTrue();

        // Unlike
        recipeRepository.unlikeRecipe(userId1, recipeId1);
        assertThat(hasLiked(userId1, recipeId1)).isFalse();

        // Like again
        recipeRepository.likeRecipe(userId1, recipeId1);
        assertThat(hasLiked(userId1, recipeId1)).isTrue();
    }

    @Test
    @DisplayName("Statistiques des likes - compter correctement après plusieurs opérations")
    void likeStatisticsAfterMultipleOperations() {
        // Given
        createUser(userId1, "user1");
        createUser(userId2, "user2");
        createUser(userId3, "user3");
        createRecipe(recipeId1);

        // When
        recipeRepository.likeRecipe(userId1, recipeId1);
        recipeRepository.likeRecipe(userId2, recipeId1);
        recipeRepository.likeRecipe(userId3, recipeId1);
        recipeRepository.unlikeRecipe(userId2, recipeId1);

        // Then
        long likeCount = recipeRepository.nbLike(recipeId1);
        assertThat(likeCount).isEqualTo(2L);
    }

    @Test
    @DisplayName("findCountsByRecipeIds should return correct counts including super likes")
    void findCountsByRecipeIdsShouldReturnCorrectCounts() {
        createUser(userId1, "user1");
        createUser(userId2, "user2");
        createRecipe(recipeId1);
        createRecipe(recipeId2);

        recipeRepository.likeRecipe(userId1, recipeId1);
        recipeRepository.likeRecipe(userId2, recipeId1);
        
        // Manually create a SUPER_LIKED relationship
        createSuperLike(userId1, recipeId2);

        var counts = recipeRepository.findCountsByRecipeIds(List.of(recipeId1.toString(), recipeId2.toString()));

        assertThat(counts).hasSize(2);
        var count1 = counts.stream().filter(c -> c.recipeId().equals(recipeId1.toString())).findFirst().orElseThrow();
        var count2 = counts.stream().filter(c -> c.recipeId().equals(recipeId2.toString())).findFirst().orElseThrow();

        assertThat(count1.likeCount()).isEqualTo(2L);
        assertThat(count1.superLikeCount()).isEqualTo(0L);
        assertThat(count2.likeCount()).isEqualTo(0L);
        assertThat(count2.superLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findUserInteractionsByRecipeIds should return correct interactions including super likes")
    void findUserInteractionsByRecipeIdsShouldReturnCorrectInteractions() {
        createUser(userId1, "user1");
        createRecipe(recipeId1);
        createRecipe(recipeId2);

        recipeRepository.likeRecipe(userId1, recipeId1);
        createSuperLike(userId1, recipeId2);

        var interactions = recipeRepository.findUserInteractionsByRecipeIds(
                List.of(recipeId1.toString(), recipeId2.toString()), userId1.toString());

        assertThat(interactions).hasSize(2);
        var interaction1 = interactions.stream().filter(i -> i.recipeId().equals(recipeId1.toString())).findFirst().orElseThrow();
        var interaction2 = interactions.stream().filter(i -> i.recipeId().equals(recipeId2.toString())).findFirst().orElseThrow();

        assertThat(interaction1.likedByCurrentUser()).isTrue();
        assertThat(interaction1.superLikedByCurrentUser()).isFalse();
        assertThat(interaction2.likedByCurrentUser()).isFalse();
        assertThat(interaction2.superLikedByCurrentUser()).isTrue();
    }

    @Test
    @DisplayName("countByAuthorId should return correct count")
    void countByAuthorIdShouldReturnCorrectCount() {
        createUser(userId1, "author1");
        createRecipe(recipeId1);
        createRecipe(recipeId2);

        // Create PUBLISHED relationship manually
        neo4jClient.query("""
                MATCH (u:User {id: $userId})
                MATCH (r1:Recipe {id: $r1})
                MATCH (r2:Recipe {id: $r2})
                MERGE (u)-[:PUBLISHED]->(r1)
                MERGE (u)-[:PUBLISHED]->(r2)
                """)
                .bindAll(Map.of("userId", userId1.toString(), "r1", recipeId1.toString(), "r2", recipeId2.toString()))
                .run();

        long count = recipeRepository.countByAuthorId(userId1);
        assertThat(count).isEqualTo(2L);
    }
    @Nested
    @DisplayName("getFeed()")
    class GetFeedTests {

        private static final ZonedDateTime NOW    = ZonedDateTime.now(ZoneOffset.UTC);
        private static final ZonedDateTime MINUS3 = NOW.minusDays(3);
        private static final ZonedDateTime PLUS1  = NOW.plusDays(1);

        private static final UUID R0 = UUID.randomUUID();
        private static final UUID R1 = UUID.randomUUID();
        private static final UUID R2 = UUID.randomUUID();
        private static final UUID R3 = UUID.randomUUID();
        private static final UUID R4 = UUID.randomUUID();


        @BeforeEach
        void setUp() {
            cleanDatabase();
            try (Session session = driver.session()) {
                session.executeWrite(tx -> {

                    tx.run("""
                                    CREATE (me:User {id: $userId})
                                    CREATE (r1:Recipe {id: $r1, title: 'Soupe', createdAt: $t1})
                                    CREATE (r2:Recipe {id: $r2, title: 'Tarte', createdAt: $t2})
                                    CREATE (r3:Recipe {id: $r3, title: 'Cake',  createdAt: $t3})
                                    CREATE (me)-[:FEED {depth: 1, createdAt: $fAt1}]->(r1)
                                    CREATE (me)-[:FEED {depth: 2, createdAt: $fAt2}]->(r2)
                                    CREATE (me)-[:FEED {depth: 1, createdAt: $fAt3}]->(r3)
                                    CREATE (r4:Recipe {id: $r4, title: 'Risotto', createdAt: $t})
                                    CREATE (me)-[:FEED {depth: 1, createdAt: $fAt}]->(r4)
                                    """,
                            Map.ofEntries(
                                    Map.entry("userId", R0.toString()),
                                    Map.entry("r1", R1.toString()),
                                    Map.entry("r2", R2.toString()),
                                    Map.entry("r3", R3.toString()),
                                    Map.entry("r4",  R4.toString()),
                                    Map.entry("t",   NOW.minusDays(1)),
                                    Map.entry("t1", NOW.minusDays(2)),
                                    Map.entry("t2", NOW.minusDays(1)),
                                    Map.entry("t3", NOW.minusDays(4)),
                                    Map.entry("fAt1", NOW.minusDays(1)),
                                    Map.entry("fAt2", NOW.minusDays(1)),
                                    Map.entry("fAt3", NOW.minusDays(5)),
                                    Map.entry("fAt", NOW.minusHours(12))
                            )
                    );
                    return null;
                });
            }
        }

        // ── Cas nominaux ───────────────────────────────────────────────────────────

        @Test
        @DisplayName("Retourne les recettes dont la relation FEED est dans la fenêtre temporelle")
        void nominal_returnsMatchingRecipes() {
            List<RecipeNode> feed = recipeRepository.getFeed(
                    R0, 0, 10, MINUS3, PLUS1
            );

            assertThat(feed).hasSize(3);
            assertThat(feed).extracting(RecipeNode::getId)
                    .containsExactlyInAnyOrder(R4, R1, R2);
        }

        @Test
        @DisplayName("Retourne une liste vide si aucune relation FEED n'existe pour cet user")
        void unknownUser_returnsEmpty() {
            List<RecipeNode> feed = recipeRepository.getFeed(
                    UUID.randomUUID(), 0, 10, MINUS3, PLUS1
            );

            assertThat(feed).isEmpty();
        }

        // ── Pagination ─────────────────────────────────────────────────────────────

        @Nested
        @DisplayName("Pagination")
        class PaginationTests {

            @Test
            @DisplayName("L'offset saute les N premiers résultats")
            void withOffset_skipsItems() {
                List<RecipeNode> feed = recipeRepository.getFeed(
                        R0, 1, 10, MINUS3, PLUS1
                );

                assertThat(feed).hasSize(2);
            }

            @Test
            @DisplayName("Limit borne correctement le nombre de résultats")
            void withLimit_truncatesResults() {
                List<RecipeNode> feed = recipeRepository.getFeed(
                        R0, 0, 1, MINUS3, PLUS1
                );

                assertThat(feed).hasSize(1);
            }

            @Test
            @DisplayName("offset >= total résultats retourne une liste vide")
            void offsetBeyondResults_returnsEmpty() {
                List<RecipeNode> feed = recipeRepository.getFeed(
                        UUID.randomUUID(), 99, 10, MINUS3, PLUS1
                );

                assertThat(feed).isEmpty();
            }
        }

        // ── Filtres temporels ──────────────────────────────────────────────────────

        @Nested
        @DisplayName("Filtres temporels")
        class TemporalFilterTests {

            @Test
            @DisplayName("sinceTime exclut les relations FEED trop anciennes")
            void sinceTime_excludesOldFeedRelations() {
                List<RecipeNode> feed = recipeRepository.getFeed(
                        UUID.randomUUID(), 0, 10,
                        NOW.minusDays(2),
                        PLUS1
                );

                assertThat(feed).extracting(RecipeNode::getId)
                        .doesNotContain(R3);
            }

            @Test
            @DisplayName("beforeTime exclut les relations FEED trop récentes")
            void beforeTime_excludesRecentFeedRelations() {
                List<RecipeNode> feed = recipeRepository.getFeed(
                        UUID.randomUUID(), 0, 10,
                        MINUS3,
                        NOW.minusDays(2)
                );

                assertThat(feed).isEmpty();
            }

            @Test
            @DisplayName("Fenêtre nulle (sinceTime == beforeTime) ne retourne rien")
            void emptyWindow_returnsEmpty() {
                ZonedDateTime point = NOW.minusSeconds(1);

                List<RecipeNode> feed = recipeRepository.getFeed(
                        UUID.randomUUID(), 0, 10, point, point
                );

                assertThat(feed).isEmpty();
            }
        }

        // ── Tri ────────────────────────────────────────────────────────────────────

        @Nested
        @DisplayName("Tri")
        class OrderingTests {

            @Test
            @DisplayName("Les recettes de depth 1 précèdent celles de depth 2")
            void depthAsc_shallowerFirst() {
                List<RecipeNode> feed = recipeRepository.getFeed(
                        R0, 0, 10, MINUS3, PLUS1
                );

                List<UUID> ids = feed.stream().map(RecipeNode::getId).toList();
                assertThat(ids.indexOf(R1)).isLessThan(ids.indexOf(R2));
            }

            @Test
            @DisplayName("À depth égal, la recette la plus récente est en tête")
            void sameDepth_newestRecipeFirst() {
                List<RecipeNode> feed = recipeRepository.getFeed(
                        R0, 0, 10, MINUS3, PLUS1
                );

                List<UUID> ids = feed.stream().map(RecipeNode::getId).toList();
                assertThat(ids.indexOf(R4)).isLessThan(ids.indexOf(R1));
            }
        }
    }
    // Helper methods

    private void createUser(UUID uuid, String username) {
        userRepository.save(new UserNode(uuid, username, "email@test.com"));
    }

    private void createRecipe(UUID uuid) {
        recipeRepository.save(new RecipeNode(uuid, "Title"));
    }

    private void createSuperLike(UUID userId, UUID recipeId) {
        neo4jClient.query("""
                MATCH (u:User {id: $userId})
                MATCH (r:Recipe {id: $recipeId})
                MERGE (u)-[:SUPER_LIKED]->(r)
                """)
                .bindAll(Map.of("userId", userId.toString(), "recipeId", recipeId.toString()))
                .run();
    }

    private void createFeed(UUID userId, UUID recipeId){
        neo4jClient.query("""
                MATCH (recipe:Recipe {id: $recipeId})
                MATCH path = (author:User {id: $authorId})<-[:FOLLOWS*1..3]-(follower:User)
                WITH follower, recipe, min(length(path)) as depth
                MERGE (follower)-[f:FEED]->(recipe)
                ON CREATE SET f.depth = depth, f.createdAt = $now
                ON MATCH SET f.depth = CASE WHEN depth < f.depth THEN depth ELSE f.depth END
                SET recipe.feed = true
            """).bindAll(Map.of("userId", userId.toString(), "recipeId", recipeId.toString()))
                .run();
    }
    private long countLikes(UUID userId, UUID recipeId) {
        return neo4jClient.query("""
                MATCH (r:Recipe {id: $recipeId})
                MATCH (u:User {id: $userId})
                OPTIONAL MATCH (r)<-[l:LIKED]-(u)
                RETURN count(l)
                """)
                .bindAll(Map.of("userId", userId.toString(), "recipeId", recipeId.toString()))
                .fetchAs(Long.class)
                .one()
                .orElse(0L);
    }

    private boolean hasLiked(UUID userId, UUID recipeId) {
        return neo4jClient.query("""
                   MATCH (p1:User {id: $userId})
                   MATCH (p2:Recipe {id: $recipeId})
                   OPTIONAL MATCH (p1)-[l:LIKED]->(p2)
                   RETURN COUNT(l) > 0
                """)
                .bindAll(Map.of("userId", userId.toString(), "recipeId", recipeId.toString()))
                .fetchAs(Boolean.class)
                .one()
                .orElse(false);
    }
}