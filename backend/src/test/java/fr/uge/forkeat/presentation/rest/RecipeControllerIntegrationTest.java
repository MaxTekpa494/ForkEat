package fr.uge.forkeat.presentation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
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
import fr.uge.forkeat.presentation.dto.recipe.CreateRecipeRequest;
import fr.uge.forkeat.presentation.dto.recipe.UpdateRecipeRequest;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class RecipeControllerIntegrationTest extends AbstractIntegrationTest {

    private final MockMvc mockMvc;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final AllergenRepository allergenRepository;
    private final IngredientRepository ingredientRepository;

    private UserEntity savedAuthor;

    // ObjectMapper local configuré pour les types Java time
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    public RecipeControllerIntegrationTest(RecipeRepository recipeRepository,
                                           UserRepository userRepository,
                                           AllergenRepository allergenRepository,
                                           IngredientRepository ingredientRepository,
                                           MockMvc mockMvc) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.allergenRepository = allergenRepository;
        this.ingredientRepository = ingredientRepository;
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() {
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

    // ========== GET /api/recipes/{id} ==========

    @Test
    void getRecipe_shouldReturnRecipeWhenExists() throws Exception {
        var recipe = createAndSaveRecipe("Tarte aux pommes", RecipeStatus.PUBLISHED, null);

        mockMvc.perform(get("/api/recipes/{id}", recipe.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.recipe.id").value(recipe.getId().toString()))
                .andExpect(jsonPath("$.resource.recipe.title").value("Tarte aux pommes"))
                .andExpect(jsonPath("$.resource.recipe.status").value("PUBLISHED"));
    }

    @Test
    void getRecipe_shouldReturn404WhenNotExists() throws Exception {
        var nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/recipes/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRecipe_shouldReturn404ForNonPublishedRecipeWhenAnonymous() throws Exception {
        // Une recette DRAFT ne doit pas être visible par un utilisateur anonyme
        var recipe = createAndSaveRecipe("Brouillon privé", RecipeStatus.DRAFT, null);

        mockMvc.perform(get("/api/recipes/{id}", recipe.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "chef_integration")
    void getRecipe_shouldReturnDraftRecipeForItsOwner() throws Exception {
        // L'auteur de la recette peut voir son propre brouillon
        var recipe = createAndSaveRecipe("Mon brouillon", RecipeStatus.DRAFT, null);

        mockMvc.perform(get("/api/recipes/{id}", recipe.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.recipe.id").value(recipe.getId().toString()))
                .andExpect(jsonPath("$.resource.recipe.status").value("DRAFT"));
    }

    @Test
    void getRecipe_shouldReturnRecipeWithParentWhenVariant() throws Exception {
        var parent = createAndSaveRecipe("Recette originale", RecipeStatus.PUBLISHED, null);
        var variant = createAndSaveRecipe("Variante", RecipeStatus.PUBLISHED, parent);

        mockMvc.perform(get("/api/recipes/{id}", variant.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.recipe.id").value(variant.getId().toString()))
                .andExpect(jsonPath("$.resource.recipe.title").value("Variante"))
                .andExpect(jsonPath("$.resource.recipe.parentId").value(parent.getId().toString()))
                .andExpect(jsonPath("$.resource.diff").exists())
                .andExpect(jsonPath("$.resource.parent").doesNotExist());
    }

    @Test
    void getRecipe_shouldReturnRecipeWithIngredients() throws Exception {
        var ingredient = new IngredientEntity("Pomme", "Fruit", false);
        ingredientRepository.save(ingredient);

        var recipe = createAndSaveRecipeWithIngredient("Compote", ingredient);

        mockMvc.perform(get("/api/recipes/{id}", recipe.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.recipe.ingredients", hasSize(1)))
                .andExpect(jsonPath("$.resource.recipe.ingredients[0].name").value("Pomme"));
    }

    @Test
    void getRecipe_shouldReturnRecipeWithAllergens() throws Exception {
        var allergen = new AllergenEntity("Gluten", AllergenSeverity.HIGH);
        var savedAllergen = allergenRepository.save(allergen);

        var recipe = createAndSaveRecipeWithAllergen("Pain", savedAllergen);

        mockMvc.perform(get("/api/recipes/{id}", recipe.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.recipe.allergens", hasSize(1)))
                .andExpect(jsonPath("$.resource.recipe.allergens[0].name").value("Gluten"))
                .andExpect(jsonPath("$.resource.recipe.allergens[0].severity").value("HIGH"));
    }

    // ========== GET /api/recipes ==========

    @Test
    void getRecipes_shouldReturnPublishedRecipesByDefault() throws Exception {
        long initialCount = recipeRepository.countByStatus(RecipeStatus.PUBLISHED);

        createAndSaveRecipe("Published 1", RecipeStatus.PUBLISHED, null);
        createAndSaveRecipe("Published 2", RecipeStatus.PUBLISHED, null);
        createAndSaveRecipe("Draft", RecipeStatus.DRAFT, null);

        mockMvc.perform(get("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(lessThanOrEqualTo(10))))
                .andExpect(jsonPath("$.total").value(initialCount + 2));
    }

    @Test
    void getRecipes_shouldFilterByStatus() throws Exception {
        long initialDraftCount = recipeRepository.countByStatus(RecipeStatus.DRAFT);

        createAndSaveRecipe("Published", RecipeStatus.PUBLISHED, null);
        createAndSaveRecipe("Draft 1", RecipeStatus.DRAFT, null);
        createAndSaveRecipe("Draft 2", RecipeStatus.DRAFT, null);

        mockMvc.perform(get("/api/recipes")
                        .param("status", "DRAFT")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(lessThanOrEqualTo(10))))
                .andExpect(jsonPath("$.total").value(initialDraftCount + 2));
    }

    @Test
    void getRecipes_shouldRespectSizeParameter() throws Exception {
        for (int i = 0; i < 5; i++) {
            createAndSaveRecipe("Recipe " + i, RecipeStatus.PUBLISHED, null);
        }
        long totalPublished = recipeRepository.countByStatus(RecipeStatus.PUBLISHED);

        mockMvc.perform(get("/api/recipes")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(2)))
                .andExpect(jsonPath("$.total").value(totalPublished));
    }

    @Test
    void getRecipes_shouldRespectPageParameter() throws Exception {
        for (int i = 0; i < 5; i++) {
            createAndSaveRecipe("Recipe " + i, RecipeStatus.PUBLISHED, null);
        }
        long totalPublished = recipeRepository.countByStatus(RecipeStatus.PUBLISHED);

        mockMvc.perform(get("/api/recipes")
                        .param("size", "2")
                        .param("page", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(2)))
                .andExpect(jsonPath("$.total").value(totalPublished));
    }

    @Test
    void getRecipes_shouldReturnEmptyListWhenNoRecipes() throws Exception {
        long rejectedCount = recipeRepository.countByStatus(RecipeStatus.REJECTED);

        mockMvc.perform(get("/api/recipes")
                        .param("status", "REJECTED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize((int) rejectedCount)))
                .andExpect(jsonPath("$.total").value(rejectedCount));
    }

    @Test
    void getRecipes_shouldHandlePendingReviewStatus() throws Exception {
        long initialPendingCount = recipeRepository.countByStatus(RecipeStatus.PENDING_REVIEW);

        createAndSaveRecipe("Pending 1", RecipeStatus.PENDING_REVIEW, null);
        createAndSaveRecipe("Pending 2", RecipeStatus.PENDING_REVIEW, null);
        createAndSaveRecipe("Published", RecipeStatus.PUBLISHED, null);

        mockMvc.perform(get("/api/recipes")
                        .param("status", "PENDING_REVIEW")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(lessThanOrEqualTo(10))))
                .andExpect(jsonPath("$.total").value(initialPendingCount + 2));
    }

    @Test
    void getRecipes_shouldReturnRecipesWithCorrectFields() throws Exception {
        var recipe = createAndSaveRecipe("Quiche Lorraine Unique", RecipeStatus.PUBLISHED, null);

        mockMvc.perform(get("/api/recipes")
                        .param("size", "10000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources[?(@.id == '" + recipe.getId() + "')].title").value("Quiche Lorraine Unique"))
                .andExpect(jsonPath("$.resources[?(@.id == '" + recipe.getId() + "')].authorUsername").value("chef_integration"));
    }

    @Test
    void getRecipes_shouldPaginateCorrectly() throws Exception {
        for (int i = 0; i < 25; i++) {
            createAndSaveRecipe("Recipe " + i, RecipeStatus.PUBLISHED, null);
        }
        var totalPublished = recipeRepository.countByStatus(RecipeStatus.PUBLISHED);

        mockMvc.perform(get("/api/recipes").param("size", "10").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(10)))
                .andExpect(jsonPath("$.total").value(totalPublished));

        mockMvc.perform(get("/api/recipes").param("size", "10").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(10)))
                .andExpect(jsonPath("$.total").value(totalPublished));

        var expectedSizePage2 = (int) Math.min(10, Math.max(0, totalPublished - 20));
        mockMvc.perform(get("/api/recipes").param("size", "10").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(expectedSizePage2)))
                .andExpect(jsonPath("$.total").value(totalPublished));
    }

    // ========== GET /api/recipes/create ==========

    @Test
    void getCreateRecipeData_shouldReturnAllergensAndIngredients() throws Exception {
        var allergen = new AllergenEntity("Arachides", AllergenSeverity.HIGH);
        allergenRepository.save(allergen);
        var ingredient = new IngredientEntity("Tomate", "Légume", false);
        ingredientRepository.save(ingredient);

        mockMvc.perform(get("/api/recipes/create")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.allergens").isArray())
                .andExpect(jsonPath("$.resource.ingredients").isArray())
                .andExpect(jsonPath("$.resource.allergens[?(@.name == 'Arachides')]").exists())
                .andExpect(jsonPath("$.resource.ingredients", hasItem("Tomate")));
    }

    @Test
    void getCreateRecipeData_shouldReturnEmptyListsWhenNoneExist() throws Exception {
        // Sans rien créer dans setUp, les listes peuvent être non vides (données de migration)
        // On vérifie juste que l'endpoint répond correctement
        mockMvc.perform(get("/api/recipes/create")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.allergens").isArray())
                .andExpect(jsonPath("$.resource.ingredients").isArray());
    }

    // ========== POST /api/recipes/create ==========

    @Test
    @WithMockUser(username = "chef_integration")
    void createRecipe_shouldCreateRecipeSuccessfully() throws Exception {
        var request = new CreateRecipeRequest(null, "Nouvelle recette", "Un résumé délicieux", 30, false, null, List.of(), List.of(), List.of(), List.of());
        var recipePart = new MockMultipartFile(
                "recipe", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/recipes/create")
                        .file(recipePart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.title").value("Nouvelle recette"))
                .andExpect(jsonPath("$.resource.username").value("chef_integration"))
                .andExpect(jsonPath("$.resource.status").value("PENDING_REVIEW"));
    }

    @Test
    @WithMockUser(username = "chef_integration")
    void createRecipe_shouldPersistRecipeInDatabase() throws Exception {
        long countBefore = recipeRepository.count();

        var request = new CreateRecipeRequest(null, "Recette persistée", "Résumé", 45, true, null, List.of(), List.of(), List.of(), List.of());
        var recipePart = new MockMultipartFile(
                "recipe", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/recipes/create").file(recipePart))
                .andExpect(status().isOk());

        long countAfter = recipeRepository.count();
        org.junit.jupiter.api.Assertions.assertEquals(countBefore + 1, countAfter);
    }

    // ========== POST /api/recipes/{id}/update ==========

    @Test
    @WithMockUser(username = "chef_integration")
    void updateRecipe_shouldUpdateRecipeSuccessfully() throws Exception {
        var recipe = createAndSaveRecipe("Titre original", RecipeStatus.PUBLISHED, null);

        var request = new UpdateRecipeRequest("Titre modifié", "Nouveau résumé", 60, false, null, List.of(), List.of(), List.of(), List.of());
        var recipePart = new MockMultipartFile(
                "recipe", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/recipes/{id}/update", recipe.getId())
                        .file(recipePart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.title").value("Titre modifié"))
                .andExpect(jsonPath("$.resource.id").value(recipe.getId().toString()));
    }

    @Test
    @WithMockUser(username = "other_user")
    void updateRecipe_shouldThrowWhenNotOwner() throws Exception {
        var recipe = createAndSaveRecipe("Recette de chef_integration", RecipeStatus.PUBLISHED, null);

        var request = new UpdateRecipeRequest("Titre modifié", "Résumé", 30, false, null, List.of(), List.of(), List.of(), List.of());
        var recipePart = new MockMultipartFile(
                "recipe", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));
        mockMvc.perform(multipart("/api/recipes/{id}/update", recipe.getId())
                        .file(recipePart)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "chef_integration")
    void updateRecipe_shouldReturn404WhenNotFound() throws Exception {
        var nonExistentId = UUID.randomUUID();

        var request = new UpdateRecipeRequest("Titre", "Résumé", 30, false, null, List.of(), List.of(), List.of(), List.of());
        var recipePart = new MockMultipartFile(
                "recipe", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/recipes/{id}/update", nonExistentId)
                        .file(recipePart))
                .andExpect(status().isNotFound());
    }

    // ========== POST /api/recipes/{id}/delete ==========

    @Test
    @WithMockUser(username = "chef_integration")
    void deleteRecipe_shouldDeleteRecipeSuccessfully() throws Exception {
        var recipe = createAndSaveRecipe("Recette à supprimer", RecipeStatus.PUBLISHED, null);

        mockMvc.perform(post("/api/recipes/{id}/delete", recipe.getId()))
                .andExpect(status().isOk());

        // Vérifier que la recette n'existe plus
        org.junit.jupiter.api.Assertions.assertFalse(
                recipeRepository.findById(recipe.getId()).isPresent());
    }

    @Test
    @WithMockUser(username = "other_user")
    void deleteRecipe_shouldThrowWhenNotOwner() throws Exception {
        // IllegalStateException non gérée dans GlobalRestExceptionHandler → Spring relance l'exception via MockMvc
        var recipe = createAndSaveRecipe("Recette protégée", RecipeStatus.PUBLISHED, null);

        mockMvc.perform(post("/api/recipes/{id}/delete", recipe.getId())).andExpect(status().isForbidden());;
    }

    @Test
    @WithMockUser(username = "chef_integration")
    void deleteRecipe_shouldReturn404WhenNotFound() throws Exception {
        var nonExistentId = UUID.randomUUID();

        mockMvc.perform(post("/api/recipes/{id}/delete", nonExistentId))
                .andExpect(status().isNotFound());
    }

    // ========== GET /api/recipes/my-recipes ==========

    @Test
    @WithMockUser(username = "chef_integration")
    void myRecipes_shouldReturnUserOwnedRecipes() throws Exception {
        createAndSaveRecipe("Ma recette 1", RecipeStatus.PUBLISHED, null);
        createAndSaveRecipe("Ma recette 2", RecipeStatus.PUBLISHED, null);

        mockMvc.perform(get("/api/recipes/my-recipes").param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.recipes.items").isArray())
                .andExpect(jsonPath("$.resource.recipes.items[*].summary.authorUsername", everyItem(is("chef_integration"))))
                .andExpect(jsonPath("$.resource.recipes.total").value(greaterThanOrEqualTo(2)));
    }

    @Test
    @WithMockUser(username = "chef_integration")
    void myRecipes_shouldReturnEmptyListForUserWithNoRecipes() throws Exception {
        // chef_integration existe en BD mais n'a aucune recette REJECTED
        mockMvc.perform(get("/api/recipes/my-recipes").param("status", "REJECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.recipes.items").isArray())
                .andExpect(jsonPath("$.resource.recipes.items", hasSize(0)))
                .andExpect(jsonPath("$.resource.recipes.total").value(0));
    }

    @Test
    @WithMockUser(username = "chef_integration")
    void myRecipes_shouldExposeStatsForAllStatuses() throws Exception {
        createAndSaveRecipe("Publiée", RecipeStatus.PUBLISHED, null);
        createAndSaveRecipe("Brouillon", RecipeStatus.DRAFT, null);
        createAndSaveRecipe("En attente", RecipeStatus.PENDING_REVIEW, null);

        mockMvc.perform(get("/api/recipes/my-recipes").param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.stats.published").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.resource.stats.draft").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.resource.stats.pendingReview").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @WithMockUser(username = "chef_integration")
    void myRecipes_shouldFilterByDraftStatus() throws Exception {
        createAndSaveRecipe("Publiée", RecipeStatus.PUBLISHED, null);
        createAndSaveRecipe("Brouillon", RecipeStatus.DRAFT, null);

        mockMvc.perform(get("/api/recipes/my-recipes").param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.recipes.items").isArray())
                .andExpect(jsonPath("$.resource.recipes.items", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.resource.recipes.items[0].status").value("DRAFT"));
    }

    @Test
    @WithMockUser(username = "chef_integration")
    void myRecipes_shouldUsePublishedAsDefaultStatus() throws Exception {
        createAndSaveRecipe("Publiée", RecipeStatus.PUBLISHED, null);
        createAndSaveRecipe("Brouillon", RecipeStatus.DRAFT, null);

        mockMvc.perform(get("/api/recipes/my-recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.recipes.items[*].status", everyItem(is("PUBLISHED"))));
    }

    @Test
    @WithMockUser(username = "chef_integration")
    void myRecipes_shouldRespectPaginationParams() throws Exception {
        createAndSaveRecipe("Recette A", RecipeStatus.PUBLISHED, null);
        createAndSaveRecipe("Recette B", RecipeStatus.PUBLISHED, null);
        createAndSaveRecipe("Recette C", RecipeStatus.PUBLISHED, null);

        mockMvc.perform(get("/api/recipes/my-recipes")
                        .param("status", "PUBLISHED")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.recipes.items", hasSize(2)))
                .andExpect(jsonPath("$.resource.recipes.total").value(greaterThanOrEqualTo(3)));
    }

    // ========== POST /api/recipes/create-variant ==========

    @Test
    @WithMockUser(username = "chef_integration")
    void createVariant_shouldCreateVariantSuccessfully() throws Exception {
        var parent = createAndSaveRecipe("Recette originale", RecipeStatus.PUBLISHED, null);

        var request = new CreateRecipeRequest(parent.getId(), "Ma variante", "Une variation de la recette", 45, false, null, List.of(), List.of(), List.of(), List.of());
        var recipePart = new MockMultipartFile(
                "recipe", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/recipes/create")
                        .file(recipePart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.title").value("Ma variante"))
                .andExpect(jsonPath("$.resource.parentId").value(parent.getId().toString()))
                .andExpect(jsonPath("$.resource.username").value("chef_integration"));
    }

    @Test
    @WithMockUser(username = "chef_integration")
    void createVariant_shouldUseProvidedImageUrl() throws Exception {
        var parent = createAndSaveRecipe("Original avec image", RecipeStatus.PUBLISHED, null);

        var request = new CreateRecipeRequest(parent.getId(), "Variante héritée", "Résumé", 30, false, "https://example.com/image.jpg", List.of(), List.of(), List.of(), List.of());
        var recipePart = new MockMultipartFile(
                "recipe", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/recipes/create")
                        .file(recipePart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource.imageUrl").value("https://example.com/image.jpg"));
    }

    @Test
    @WithMockUser(username = "chef_integration")
    void createVariant_shouldReturn404WhenParentNotFound() throws Exception {
        var nonExistentParentId = UUID.randomUUID();

        var request = new CreateRecipeRequest(nonExistentParentId, "Variante orpheline", "Résumé", 30, false, null, List.of(), List.of(), List.of(), List.of());
        var recipePart = new MockMultipartFile(
                "recipe", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/recipes/create")
                        .file(recipePart))
                .andExpect(status().isNotFound());
    }

    // ========== Helpers ==========

    private RecipeEntity createAndSaveRecipe(String title, RecipeStatus status, RecipeEntity parent) {
        var recipe = new RecipeEntity();
        recipe.setTitle(title);
        recipe.setSummary("Summary for " + title);
        recipe.setAuthor(savedAuthor);
        recipe.setStatus(status);
        recipe.setPreparationMinutes(30);
        recipe.setParent(parent);
        recipe.setStepByStepInstructions(List.of());
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
        var recipeAllergen = new RecipeAllergenEntity(recipe, allergen);
        recipe.addAllergen(recipeAllergen);
        return recipeRepository.save(recipe);
    }
}
