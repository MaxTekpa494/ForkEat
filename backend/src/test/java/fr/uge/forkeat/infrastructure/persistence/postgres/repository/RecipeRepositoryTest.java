package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeStep;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RecipeRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("forkeat_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "&stringtype=unspecified");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity savedAuthor;

    @BeforeEach
    void setUp() {
        recipeRepository.deleteAll();
        userRepository.deleteAll();

        var author = new UserEntity();
        author.setUsername("chef_test");
        author.setEmail("chef@example.com");
        author.setFirstName("Chef");
        author.setLastName("Test");
        author.setPassword("password123");
        author.setRole(UserRole.MEMBER);
        author.setStatus(UserStatus.ACTIVE);
        author.setAuthMode(AuthMode.LOCAL);
        savedAuthor = userRepository.save(author);
    }

    private RecipeEntity createRecipe(String title, String source, String externalId, RecipeStatus status) {
        var recipe = new RecipeEntity();
        recipe.setTitle(title);
        recipe.setSummary("Summary for " + title);
        recipe.setSource(source);
        recipe.setExternalId(externalId);
        recipe.setStatus(status);
        recipe.setAuthor(savedAuthor);
        recipe.setStepByStepInstructions(List.of(new RecipeStep(1, "First step")));
        recipe.setDietaryFlag(Map.of("vegetarian", false, "vegan", false));
        return recipe;
    }

    @Test
    void shouldSaveAndFindRecipeById() {
        var recipe = createRecipe("Pasta Carbonara", "internal", null, RecipeStatus.PUBLISHED);
        var saved = recipeRepository.save(recipe);

        var found = recipeRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Pasta Carbonara", found.get().getTitle());
    }

    @Test
    void shouldFindBySourceAndExternalId() {
        var recipe = createRecipe("External Recipe", "spoonacular", "12345", RecipeStatus.PUBLISHED);
        recipeRepository.save(recipe);

        var found = recipeRepository.findBySourceAndExternalId("spoonacular", "12345");

        assertTrue(found.isPresent());
        assertEquals("External Recipe", found.get().getTitle());
    }

    @Test
    void shouldReturnEmptyWhenSourceAndExternalIdNotFound() {
        var found = recipeRepository.findBySourceAndExternalId("unknown", "99999");

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldCheckIfExistsBySourceAndExternalId() {
        var recipe = createRecipe("API Recipe", "api", "api-001", RecipeStatus.DRAFT);
        recipeRepository.save(recipe);

        assertTrue(recipeRepository.existsBySourceAndExternalId("api", "api-001"));
        assertFalse(recipeRepository.existsBySourceAndExternalId("api", "api-999"));
    }

    @Test
    void shouldFindBySource() {
        recipeRepository.save(createRecipe("Recipe 1", "spoonacular", "1", RecipeStatus.PUBLISHED));
        recipeRepository.save(createRecipe("Recipe 2", "spoonacular", "2", RecipeStatus.PUBLISHED));
        recipeRepository.save(createRecipe("Recipe 3", "internal", null, RecipeStatus.DRAFT));

        var spoonacularRecipes = recipeRepository.findBySource("spoonacular");

        assertEquals(2, spoonacularRecipes.size());
    }

    @Test
    void shouldFindByStatus() {
        recipeRepository.save(createRecipe("Draft Recipe", "internal", null, RecipeStatus.DRAFT));
        recipeRepository.save(createRecipe("Published Recipe 1", "internal", null, RecipeStatus.PUBLISHED));
        recipeRepository.save(createRecipe("Published Recipe 2", "internal", null, RecipeStatus.PUBLISHED));

        var publishedRecipes = recipeRepository.findByStatus(RecipeStatus.PUBLISHED);
        var draftRecipes = recipeRepository.findByStatus(RecipeStatus.DRAFT);

        assertEquals(2, publishedRecipes.size());
        assertEquals(1, draftRecipes.size());
    }

    @Test
    void shouldFindByAuthorId() {
        recipeRepository.save(createRecipe("Author Recipe 1", "internal", null, RecipeStatus.PUBLISHED));
        recipeRepository.save(createRecipe("Author Recipe 2", "internal", null, RecipeStatus.DRAFT));

        var authorRecipes = recipeRepository.findByAuthorId(savedAuthor.getId());

        assertEquals(2, authorRecipes.size());
    }

    @Test
    void shouldCountBySource() {
        recipeRepository.save(createRecipe("Recipe 1", "spoonacular", "1", RecipeStatus.PUBLISHED));
        recipeRepository.save(createRecipe("Recipe 2", "spoonacular", "2", RecipeStatus.PUBLISHED));
        recipeRepository.save(createRecipe("Recipe 3", "internal", null, RecipeStatus.DRAFT));

        assertEquals(2, recipeRepository.countBySource("spoonacular"));
        assertEquals(1, recipeRepository.countBySource("internal"));
        assertEquals(0, recipeRepository.countBySource("unknown"));
    }

    @Test
    void shouldFindByTitleContainingIgnoreCase() {
        recipeRepository.save(createRecipe("Pasta Carbonara", "internal", null, RecipeStatus.PUBLISHED));
        recipeRepository.save(createRecipe("Spaghetti Bolognese", "internal", null, RecipeStatus.PUBLISHED));
        recipeRepository.save(createRecipe("pasta primavera", "internal", null, RecipeStatus.DRAFT));

        var pastaRecipes = recipeRepository.findByTitleContainingIgnoreCase("pasta");

        assertEquals(2, pastaRecipes.size());
    }

    @Test
    void shouldCheckIfExistsByTitle() {
        recipeRepository.save(createRecipe("Unique Recipe", "internal", null, RecipeStatus.PUBLISHED));

        assertTrue(recipeRepository.existsByTitle("Unique Recipe"));
        assertFalse(recipeRepository.existsByTitle("Non Existent Recipe"));
    }

    @Test
    void shouldFindByStatusIn() {
        recipeRepository.save(createRecipe("Draft", "internal", null, RecipeStatus.DRAFT));
        recipeRepository.save(createRecipe("Published", "internal", null, RecipeStatus.PUBLISHED));
        recipeRepository.save(createRecipe("Pending", "internal", null, RecipeStatus.PENDING_REVIEW));
        recipeRepository.save(createRecipe("Rejected", "internal", null, RecipeStatus.REJECTED));

        var activeRecipes = recipeRepository.findByStatusIn(List.of(RecipeStatus.PUBLISHED, RecipeStatus.DRAFT));

        assertEquals(2, activeRecipes.size());
    }

    @Test
    void shouldCountByStatus() {
        recipeRepository.save(createRecipe("Published 1", "internal", null, RecipeStatus.PUBLISHED));
        recipeRepository.save(createRecipe("Published 2", "internal", null, RecipeStatus.PUBLISHED));
        recipeRepository.save(createRecipe("Draft", "internal", null, RecipeStatus.DRAFT));

        assertEquals(2, recipeRepository.countByStatus(RecipeStatus.PUBLISHED));
        assertEquals(1, recipeRepository.countByStatus(RecipeStatus.DRAFT));
        assertEquals(0, recipeRepository.countByStatus(RecipeStatus.REJECTED));
    }

    @Test
    void shouldCountByAuthorId() {
        recipeRepository.save(createRecipe("Recipe 1", "internal", null, RecipeStatus.PUBLISHED));
        recipeRepository.save(createRecipe("Recipe 2", "internal", null, RecipeStatus.DRAFT));

        assertEquals(2, recipeRepository.countByAuthorId(savedAuthor.getId()));
    }

    @Test
    void shouldFindByAuthorIdAndStatus() {
        recipeRepository.save(createRecipe("Published Recipe", "internal", null, RecipeStatus.PUBLISHED));
        recipeRepository.save(createRecipe("Draft Recipe", "internal", null, RecipeStatus.DRAFT));

        var publishedByAuthor = recipeRepository.findByAuthorIdAndStatus(savedAuthor.getId(), RecipeStatus.PUBLISHED);
        var draftByAuthor = recipeRepository.findByAuthorIdAndStatus(savedAuthor.getId(), RecipeStatus.DRAFT);

        assertEquals(1, publishedByAuthor.size());
        assertEquals("Published Recipe", publishedByAuthor.getFirst().getTitle());
        assertEquals(1, draftByAuthor.size());
        assertEquals("Draft Recipe", draftByAuthor.getFirst().getTitle());
    }
}
