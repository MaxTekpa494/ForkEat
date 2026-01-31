package fr.uge.forkeat.presentation.rest;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.AllergenEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.IngredientEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeAllergenEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeIngredientEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.AllergenRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.IngredientRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.recipe.AllergenSeverity;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional // Chaque test est dans une transaction, rollback automatique à la fin
class RecipeControllerIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AllergenRepository allergenRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    private UserEntity savedAuthor;

    @BeforeEach
    void setUp() {
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        allergenRepository.deleteAll();
        userRepository.deleteAll();

        var author = new UserEntity();
        author.setUsername("chef_integration");
        author.setEmail("chef@integration.com");
        author.setFirstName("Chef");
        author.setLastName("Integration");
        author.setPassword("password123");
        author.setRole(UserRole.MEMBER);
        author.setStatus(UserStatus.ACTIVE);
        author.setAuthMode(AuthMode.LOCAL);
        savedAuthor = userRepository.save(author);
    }

    // ========== GET /api/recipes/{id} tests ==========

    @Test
    void getRecipe_shouldReturnRecipeWhenExists() throws Exception {
        var recipe = createAndSaveRecipe("Tarte aux pommes", RecipeStatus.PUBLISHED, null);

        mockMvc.perform(get("/api/recipes/{id}", recipe.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.id").value(recipe.getId().toString()))
                .andExpect(jsonPath("$.resource.title").value("Tarte aux pommes"))
                .andExpect(jsonPath("$.resource.status").value("PUBLISHED"));
    }

    @Test
    void getRecipe_shouldReturn404WhenNotExists() throws Exception {
        var nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/recipes/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRecipe_shouldReturnRecipeWithParentWhenVariant() throws Exception {
        var parent = createAndSaveRecipe("Recette originale", RecipeStatus.PUBLISHED, null);
        var variant = createAndSaveRecipe("Variante", RecipeStatus.DRAFT, parent);

        mockMvc.perform(get("/api/recipes/{id}", variant.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.id").value(variant.getId().toString()))
                .andExpect(jsonPath("$.resource.title").value("Variante"))
                .andExpect(jsonPath("$.resource.parent").isNotEmpty())
                .andExpect(jsonPath("$.resource.parent.id").value(parent.getId().toString()))
                .andExpect(jsonPath("$.resource.parent.title").value("Recette originale"));
    }

    @Test
    void getRecipe_shouldReturnRecipeWithIngredients() throws Exception {
        var ingredient = new IngredientEntity("Pomme", "Fruit", false);
        ingredientRepository.save(ingredient);

        var recipe = createAndSaveRecipeWithIngredient("Compote", ingredient);

        mockMvc.perform(get("/api/recipes/{id}", recipe.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.ingredients", hasSize(1)))
                .andExpect(jsonPath("$.resource.ingredients[0].name").value("Pomme"));
    }

    @Test
    void getRecipe_shouldReturnRecipeWithAllergens() throws Exception {
        var allergen = new AllergenEntity("Gluten", AllergenSeverity.HIGH);
        var savedAllergen = allergenRepository.save(allergen);

        var recipe = createAndSaveRecipeWithAllergen("Pain", savedAllergen);

        mockMvc.perform(get("/api/recipes/{id}", recipe.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.allergens", hasSize(1)))
                .andExpect(jsonPath("$.resource.allergens[0].name").value("Gluten"))
                .andExpect(jsonPath("$.resource.allergens[0].severity").value("HIGH"));
    }

    // ========== GET /api/recipes tests ==========

    @Test
    void getRecipes_shouldReturnPublishedRecipesByDefault() throws Exception {
        createAndSaveRecipe("Published 1", RecipeStatus.PUBLISHED, null);
        createAndSaveRecipe("Published 2", RecipeStatus.PUBLISHED, null);
        createAndSaveRecipe("Draft", RecipeStatus.DRAFT, null);

        mockMvc.perform(get("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(2)))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void getRecipes_shouldFilterByStatus() throws Exception {
        createAndSaveRecipe("Published", RecipeStatus.PUBLISHED, null);
        createAndSaveRecipe("Draft 1", RecipeStatus.DRAFT, null);
        createAndSaveRecipe("Draft 2", RecipeStatus.DRAFT, null);

        mockMvc.perform(get("/api/recipes")
                        .param("status", "DRAFT")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(2)))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.resources[*].status", everyItem(is("DRAFT"))));
    }

    @Test
    void getRecipes_shouldRespectSizeParameter() throws Exception {
        for (int i = 0; i < 5; i++) {
            createAndSaveRecipe("Recipe " + i, RecipeStatus.PUBLISHED, null);
        }

        mockMvc.perform(get("/api/recipes")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(2)))
                .andExpect(jsonPath("$.total").value(5));
    }

    @Test
    void getRecipes_shouldRespectPageParameter() throws Exception {
        for (int i = 0; i < 5; i++) {
            createAndSaveRecipe("Recipe " + i, RecipeStatus.PUBLISHED, null);
        }

        // 5 recettes, size=2, page=1 → skip 2 premières, retourne 2 suivantes
        mockMvc.perform(get("/api/recipes")
                        .param("size", "2")
                        .param("page", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(2)))
                .andExpect(jsonPath("$.total").value(5));
    }

    @Test
    void getRecipes_shouldReturnEmptyListWhenNoRecipes() throws Exception {
        mockMvc.perform(get("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void getRecipes_shouldHandlePendingReviewStatus() throws Exception {
        createAndSaveRecipe("Pending 1", RecipeStatus.PENDING_REVIEW, null);
        createAndSaveRecipe("Pending 2", RecipeStatus.PENDING_REVIEW, null);
        createAndSaveRecipe("Published", RecipeStatus.PUBLISHED, null);

        mockMvc.perform(get("/api/recipes")
                        .param("status", "PENDING_REVIEW")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(2)))
                .andExpect(jsonPath("$.resources[*].status", everyItem(is("PENDING_REVIEW"))));
    }

    @Test
    void getRecipes_shouldReturnRecipesWithCorrectFields() throws Exception {
        var recipe = createAndSaveRecipe("Quiche Lorraine", RecipeStatus.PUBLISHED, null);

        mockMvc.perform(get("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources[0].id").value(recipe.getId().toString()))
                .andExpect(jsonPath("$.resources[0].title").value("Quiche Lorraine"))
                .andExpect(jsonPath("$.resources[0].summary").exists())
                .andExpect(jsonPath("$.resources[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.resources[0].username").value("chef_integration"));
    }

    @Test
    void getRecipes_shouldPaginateCorrectly() throws Exception {
        for (int i = 0; i < 25; i++) {
            createAndSaveRecipe("Recipe " + i, RecipeStatus.PUBLISHED, null);
        }

        // First page (page=0)
        mockMvc.perform(get("/api/recipes")
                        .param("size", "10")
                        .param("page", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(10)))
                .andExpect(jsonPath("$.total").value(25));

        // Second page (page=1)
        mockMvc.perform(get("/api/recipes")
                        .param("size", "10")
                        .param("page", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(10)))
                .andExpect(jsonPath("$.total").value(25));

        // Third page (page=2, partial - only 5 remaining)
        mockMvc.perform(get("/api/recipes")
                        .param("size", "10")
                        .param("page", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(5)))
                .andExpect(jsonPath("$.total").value(25));
    }

    // ========== Helper methods ==========

    private RecipeEntity createAndSaveRecipe(String title, RecipeStatus status, RecipeEntity parent) {
        var recipe = new RecipeEntity();
        recipe.setTitle(title);
        recipe.setSummary("Summary for " + title);
        recipe.setAuthor(savedAuthor);
        recipe.setStatus(status);
        recipe.setPreparationMinutes(30);
        recipe.setParent(parent);
        recipe.setStepByStepInstructions(List.of());
        recipe.setDietaryFlag(new java.util.HashMap<>());
        return recipeRepository.save(recipe);
    }

    private RecipeEntity createAndSaveRecipeWithIngredient(String title, IngredientEntity ingredient) {
        var recipe = new RecipeEntity();
        recipe.setTitle(title);
        recipe.setSummary("Summary for " + title);
        recipe.setAuthor(savedAuthor);
        recipe.setStatus(RecipeStatus.PUBLISHED);
        recipe.setPreparationMinutes(30);
        recipe.setStepByStepInstructions(List.of());
        recipe.setDietaryFlag(new java.util.HashMap<>());
        var recipeIngredient = new RecipeIngredientEntity(recipe, ingredient, java.math.BigDecimal.valueOf(500.0), "g");
        recipe.addIngredient(recipeIngredient);
        return recipeRepository.save(recipe);
    }

    private RecipeEntity createAndSaveRecipeWithAllergen(String title, AllergenEntity allergen) {
        var recipe = new RecipeEntity();
        recipe.setTitle(title);
        recipe.setSummary("Summary for " + title);
        recipe.setAuthor(savedAuthor);
        recipe.setStatus(RecipeStatus.PUBLISHED);
        recipe.setPreparationMinutes(30);
        recipe.setStepByStepInstructions(List.of());
        recipe.setDietaryFlag(new java.util.HashMap<>());
        var recipeAllergen = new RecipeAllergenEntity(recipe, allergen);
        recipe.addAllergen(recipeAllergen);
        return recipeRepository.save(recipe);
    }
}
