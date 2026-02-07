package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class RecipeWebControllerIntegrationTest {

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

    private UserEntity savedAuthor;


    @BeforeEach
    void setUp() {
        recipeRepository.deleteAll();

        var author = new UserEntity();
        author.setUsername("chef_web");
        author.setEmail("chef@web.com");
        author.setFirstName("Chef");
        author.setLastName("Web");
        author.setPassword("password123");
        author.setRole(UserRole.MEMBER);
        author.setStatus(UserStatus.ACTIVE);
        author.setAuthMode(AuthMode.LOCAL);
        savedAuthor = userRepository.save(author);
    }

    // ========== GET /recipes (listRecipes) ==========

    @Test
    void listRecipes_shouldReturnIndexViewWithPublishedRecipes() throws Exception {
        createAndSaveRecipe("Tarte aux pommes", RecipeStatus.PUBLISHED, null);
        createAndSaveRecipe("Quiche Lorraine", RecipeStatus.PUBLISHED, null);
        createAndSaveRecipe("Brouillon", RecipeStatus.DRAFT, null);

        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/index"))
                .andExpect(model().attributeExists("recipes"))
                .andExpect(model().attribute("totalRecipes", 2L))
                .andExpect(model().attribute("currentPage", 0));
    }

    @Test
    void listRecipes_shouldFilterByDraftStatus() throws Exception {
        createAndSaveRecipe("Published", RecipeStatus.PUBLISHED, null);
        createAndSaveRecipe("Draft 1", RecipeStatus.DRAFT, null);
        createAndSaveRecipe("Draft 2", RecipeStatus.DRAFT, null);

        mockMvc.perform(get("/recipes").param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/index"))
                .andExpect(model().attribute("totalRecipes", 2L));
    }

    @Test
    void listRecipes_shouldRespectPagination() throws Exception {
        for (int i = 0; i < 5; i++) {
            createAndSaveRecipe("Recipe " + i, RecipeStatus.PUBLISHED, null);
        }

        mockMvc.perform(get("/recipes")
                        .param("size", "2")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalRecipes", 5L))
                .andExpect(model().attribute("totalPages", 3))
                .andExpect(model().attribute("currentPage", 0));
    }

    @Test
    void listRecipes_shouldReturnEmptyListWhenNoRecipes() throws Exception {
        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/index"))
                .andExpect(model().attribute("totalRecipes", 0L))
                .andExpect(model().attribute("totalPages", 0));
    }

    @Test
    void listRecipes_shouldPassSearchParamToModel() throws Exception {
        mockMvc.perform(get("/recipes").param("search", "tarte"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("search", "tarte"));
    }

    // ========== GET /recipes/{id} (viewRecipe) ==========

    @Test
    void viewRecipe_shouldReturnDetailViewWhenRecipeExists() throws Exception {
        var recipe = createAndSaveRecipe("Tarte aux pommes", RecipeStatus.PUBLISHED, null);

        mockMvc.perform(get("/recipes/{id}", recipe.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/detail"))
                .andExpect(model().attributeExists("recipe"));
    }

    @Test
    void viewRecipe_shouldReturn404WhenRecipeNotFound() throws Exception {
        var nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/recipes/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void viewRecipe_shouldIncludeParentWhenRecipeIsVariant() throws Exception {
        var parent = createAndSaveRecipe("Recette originale", RecipeStatus.PUBLISHED, null);
        var variant = createAndSaveRecipe("Variante", RecipeStatus.PUBLISHED, parent);

        mockMvc.perform(get("/recipes/{id}", variant.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("recipes/detail"))
                .andExpect(model().attributeExists("recipe"))
                .andExpect(model().attributeExists("parent"));
    }

    @Test
    void viewRecipe_shouldNotIncludeParentWhenRecipeIsNotVariant() throws Exception {
        var recipe = createAndSaveRecipe("Tarte classique", RecipeStatus.PUBLISHED, null);

        mockMvc.perform(get("/recipes/{id}", recipe.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("parent"));
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
        recipe.setDietaryFlag(new HashMap<>());
        return recipeRepository.save(recipe);
    }
}
