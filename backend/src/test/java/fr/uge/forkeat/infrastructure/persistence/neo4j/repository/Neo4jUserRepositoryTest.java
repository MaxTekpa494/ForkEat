package fr.uge.forkeat.infrastructure.persistence.neo4j.repository;

import org.junit.jupiter.api.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.neo4j.test.autoconfigure.DataNeo4jTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataNeo4jTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class Neo4jUserRepositoryTest {

    @Autowired
    private Neo4jUserRepository userRepository;

    @Autowired
    private Neo4jRecipeRepository recipeRepository;

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

        // Initialize test UUIDs


        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        userId3 = UUID.randomUUID();
        recipeId1 = UUID.randomUUID();
        recipeId2 = UUID.randomUUID();
    }


    private void cleanDatabase() {
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n");
        }
    }

    @Test
    @DisplayName("Should create a new LIKED relationship and return true")
    void shouldCreateNewLike() {
        createUser(userId1);
        createRecipe(recipeId1);

        userRepository.likeRecipe(userId1, recipeId1);

        assertTrue(userRepository.hasLiked(userId1, recipeId1), "Like relationship should exist");
    }

    @Test
    @DisplayName("Multiple users can like the same recipe")
    void multipleUsersCanLikeSameRecipe() {
        createUser(userId1);
        createUser(userId2);
        createRecipe(recipeId1);
        // When
        userRepository.likeRecipe(userId1, recipeId1);
        userRepository.likeRecipe(userId2, recipeId1);

        // Then
        long likeCount =recipeRepository.nbLike(recipeId1);
        assertThat(likeCount).isEqualTo(2L);
    }


    @Test
    @DisplayName("A user can like multiple recipes")
    void userCanLikeMultipleRecipes() {
        // Given
        createUser(userId1);
        createRecipe(recipeId1);
        createRecipe(recipeId2);

        // When
        userRepository.likeRecipe(userId1, recipeId1);
        userRepository.likeRecipe(userId1, recipeId2);

        // Then
        long userLikeCount = countLikes(userId1, recipeId1);
        long userLikeCount2 = countLikes(userId1, recipeId2);
        assertThat(userLikeCount).isEqualTo(1L);
        assertThat(userLikeCount2).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should delete relation")
    void shouldDeleteExistingLikedRelationship() {

        createUser(userId1);
        createRecipe(recipeId1);
        // Given
        userRepository.likeRecipe(userId1, recipeId1);
        assertThat(userRepository.hasLiked(userId1, recipeId1)).isTrue();

        // When
        userRepository.unlikeRecipe(userId1, recipeId1);

        // Then
        boolean hasLiked = userRepository.hasLiked(userId1, recipeId1);
        assertThat(hasLiked).isFalse();
    }

    @Test
    @DisplayName("Should be idempotent , should not throw error")
    void shouldBeIdempotent() {

        createUser(userId1);
        createRecipe(recipeId1);

        // Given
        userRepository.likeRecipe(userId1, recipeId1);

        // When/Then - ne devrait pas lancer d'exception
        Assertions.assertDoesNotThrow(() -> {
            userRepository.unlikeRecipe(userId1, recipeId1);
            userRepository.unlikeRecipe(userId1, recipeId1);
        });

        boolean hasLiked = userRepository.hasLiked(userId1, recipeId1);
        assertThat(hasLiked).isFalse();
    }


    @Test
    @DisplayName("Should not affect other relations")
    void shouldNotAffectOtherRelationships() {
        // Given
        createUser(userId1);
        createUser(userId2);
        createRecipe(recipeId1);

        userRepository.likeRecipe(userId1, recipeId1);
        userRepository.likeRecipe(userId2, recipeId1);

        // When
        userRepository.unlikeRecipe(userId1, recipeId1);

        // Then
        assertThat(userRepository.hasLiked(userId1, recipeId1)).isFalse();
        assertThat(userRepository.hasLiked(userId2, recipeId1)).isTrue();
        assertThat(recipeRepository.nbLike(recipeId1)).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should not remove recipe or user")
    void shouldNotDeleteUserOrRecipe() {
       createUser(userId1);
       createRecipe(recipeId1);

        // Given
        userRepository.likeRecipe(userId1, recipeId1);

        // When
        userRepository.unlikeRecipe(userId1, recipeId1);

        // Then
        assertThat(userRepository.findById(userId1)).isPresent();
        assertThat(recipeRepository.findById(recipeId1)).isPresent();
    }


    @Test
    @DisplayName("Should return false when like already exists")
    void shouldNotCreateDuplicateLike() {
        createUser(userId1);
        createRecipe(recipeId1);

        userRepository.likeRecipe(userId1, recipeId1);
        userRepository.likeRecipe(userId1, recipeId1);

        assertEquals(1, countLikes(userId1, recipeId1), "Should have exactly one like");
    }

    @Test
    @DisplayName("hasLiked should return false when no like exists")
    void hasLikedShouldReturnFalseWhenNoLike() {
        createUser(userId1);
        createRecipe(recipeId1);

        assertFalse(userRepository.hasLiked(userId1, recipeId1),
                "Should return false when no like exists");
    }

    @Test
    @DisplayName("hasLiked should return true after liking")
    void hasLikedShouldReturnTrueAfterLiking() {
        createUser(userId1);
        createRecipe(recipeId1);

        userRepository.likeRecipe(userId1, recipeId1);

        assertTrue(userRepository.hasLiked(userId1, recipeId1),
                "Should return true after liking");
    }

    @Test
    @DisplayName("Should handle concurrent like attempts without duplicates")
    void shouldHandleConcurrentLikes() throws InterruptedException {
        createUser(userId1);
        createRecipe(recipeId1);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        // Launch concurrent like attempts
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    userRepository.likeRecipe(userId1, recipeId1);
                } catch (Exception e) {
                    fail("Should not throw exception: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads at once

        executor.shutdown();
        //Waiting all threads finish
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(1, countLikes(userId1, recipeId1),
                "Should have exactly one like in database");
        assertTrue(userRepository.hasLiked(userId1, recipeId1),
                "Like should exist");
    }

    @Test
    @DisplayName("Should allow different users to like the same recipe")
    void shouldAllowMultipleUsersToLikeSameRecipe() {
        createUser(userId1);
        createUser(userId2);
        createRecipe(recipeId1);

        userRepository.likeRecipe(userId1, recipeId1);
        userRepository.likeRecipe(userId2, recipeId1);


        assertTrue(userRepository.hasLiked(userId1, recipeId1),
                "User1 like should exist");
        assertTrue(userRepository.hasLiked(userId2, recipeId1),
                "User2 like should exist");
    }

    @Test
    @DisplayName("Should allow same user to like different recipes")
    void shouldAllowUserToLikeMultipleRecipes() {
        createUser(userId1);
        createRecipe(recipeId1);
        createRecipe(recipeId2);

        userRepository.likeRecipe(userId1, recipeId1);
        userRepository.likeRecipe(userId1, recipeId2);

        assertTrue(userRepository.hasLiked(userId1, recipeId1),
                "Like for recipe1 should exist");
        assertTrue(userRepository.hasLiked(userId1, recipeId2),
                "Like for recipe2 should exist");
    }

    @Test
    @DisplayName("Should not create duplicate likes for same user and recipe")
    void shouldNotCreateDuplicateLikesMultipleTimes() {
        createUser(userId1);
        createRecipe(recipeId1);

        userRepository.likeRecipe(userId1, recipeId1);
        userRepository.likeRecipe(userId1, recipeId1);
        userRepository.likeRecipe(userId1, recipeId1);

        assertEquals(1, countLikes(userId1, recipeId1),
                "Should have exactly one like");
    }
    @Test
    @DisplayName("Should handle rapid successive likes correctly")
    void shouldHandleRapidSuccessiveLikes() throws InterruptedException {
        createUser(userId1);
        createRecipe(recipeId1);

        userRepository.likeRecipe(userId1, recipeId1);
        Thread.sleep(1); // Minimal delay
        userRepository.likeRecipe(userId1, recipeId1);
        Thread.sleep(1);
        userRepository.likeRecipe(userId1, recipeId1);

        assertEquals(1, countLikes(userId1, recipeId1));
    }

    @Test
    @DisplayName("findById should work correctly")
    void shouldFindUserById() {
        createUser(userId1);

        var user = userRepository.findById(userId1);

        assertTrue(user.isPresent(), "User should be found");
        assertEquals(userId1, user.get().getId(), "UUID should match");
    }

    @Test
    @DisplayName("findById should return empty for non-existent user")
    void shouldReturnEmptyForNonExistentUser() {
        UUID nonExistentId = UUID.randomUUID();

        var user = userRepository.findById(nonExistentId);

        assertFalse(user.isPresent(), "Should return empty for non-existent user");
    }

    @Test
    @DisplayName("Scénario complet: Like -> Unlike -> Like")
    void completeLikeUnlikeLikeScenario() {
        createUser(userId1);
        createRecipe(recipeId1);

        // Like
        userRepository.likeRecipe(userId1, recipeId1);
        assertThat(userRepository.hasLiked(userId1, recipeId1)).isTrue();

        // Unlike
        userRepository.unlikeRecipe(userId1, recipeId1);
        assertThat(userRepository.hasLiked(userId1, recipeId1)).isFalse();

        // Like again
        userRepository.likeRecipe(userId1, recipeId1);
        assertThat(userRepository.hasLiked(userId1, recipeId1)).isTrue();
    }

    @Test
    @DisplayName("Statistiques des likes - compter correctement après plusieurs opérations")
    void likeStatisticsAfterMultipleOperations() {
        // Given
        createUser(userId1);
        createUser(userId2);
        createUser(userId3);
        createRecipe(recipeId1);

        // When
        userRepository.likeRecipe(userId1, recipeId1);
        userRepository.likeRecipe(userId2, recipeId1);
        userRepository.likeRecipe(userId3, recipeId1);
        userRepository.unlikeRecipe(userId2, recipeId1);

        // Then
        long likeCount = recipeRepository.nbLike(recipeId1);
        assertThat(likeCount).isEqualTo(2L);
    }

    // Helper methods

    private void createUser(UUID uuid) {
        try (Session session = driver.session()) {
            session.run("CREATE (u:User {id: $uuid})",
                    java.util.Map.of("uuid", uuid.toString()));
        }
    }

    private void createRecipe(UUID uuid) {
        try (Session session = driver.session()) {
            session.run("CREATE (r:Recipe {id: $uuid})",
                    java.util.Map.of("uuid", uuid.toString()));
        }
    }

    private long countLikes(UUID userId, UUID recipeId) {
        try (Session session = driver.session()) {
            var result = session.run("""        
                    MATCH (r:Recipe {id: $recipeId})
                    MATCH (u:User {id: $userId})
                    OPTIONAL MATCH (r)<-[:LIKED]-(u)
                    RETURN count(*) AS nbLike
            """,
                    java.util.Map.of("userId", userId.toString(), "recipeId", recipeId.toString())
            );
            return result.single().get("nbLike").asLong();
        }
    }
}