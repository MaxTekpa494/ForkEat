package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.presentation.web.viewmodel.RecipeListViewModel;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class RecipeWebControllerIntegrationTest extends AbstractIntegrationTest {

    private final MockMvc mockMvc;

    private final RecipeRepository recipeRepository;

    private final UserRepository userRepository;

    private UserEntity savedAuthor;

    @Autowired
    public RecipeWebControllerIntegrationTest(UserRepository userRepository, RecipeRepository recipeRepository, MockMvc mockMvc) {
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() {
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

    @Nested
    class ListRecipes {

        @Test
        void shouldReturnIndexViewWithPublishedRecipes() throws Exception {
            createAndSaveRecipe("Tarte aux pommes", RecipeStatus.PUBLISHED, null);
            createAndSaveRecipe("Quiche Lorraine", RecipeStatus.PUBLISHED, null);
            createAndSaveRecipe("Brouillon", RecipeStatus.DRAFT, null);

            var result = mockMvc.perform(get("/recipes"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("recipes/index"))
                    .andExpect(model().attributeExists("vm"))
                    .andReturn();

            var vm = (RecipeListViewModel) result.getModelAndView().getModel().get("vm");
            assertEquals(2L, vm.totalRecipes());
            assertEquals(0, vm.currentPage());
        }

        @Test
        void shouldFilterByDraftStatus() throws Exception {
            createAndSaveRecipe("Published", RecipeStatus.PUBLISHED, null);
            createAndSaveRecipe("Draft 1", RecipeStatus.DRAFT, null);
            createAndSaveRecipe("Draft 2", RecipeStatus.DRAFT, null);

            var result = mockMvc.perform(get("/recipes").param("status", "DRAFT"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("recipes/index"))
                    .andReturn();

            var vm = (RecipeListViewModel) result.getModelAndView().getModel().get("vm");
            assertEquals(2L, vm.totalRecipes());
        }

        @Test
        void shouldRespectPagination() throws Exception {
            for (int i = 0; i < 5; i++) {
                createAndSaveRecipe("Recipe " + i, RecipeStatus.PUBLISHED, null);
            }

            var result = mockMvc.perform(get("/recipes")
                            .param("size", "2")
                            .param("page", "0"))
                    .andExpect(status().isOk())
                    .andReturn();

            var vm = (RecipeListViewModel) result.getModelAndView().getModel().get("vm");
            assertEquals(5L, vm.totalRecipes());
            assertEquals(3, vm.totalPages());
            assertEquals(0, vm.currentPage());
        }

        @Test
        void shouldReturnEmptyListWhenNoRecipes() throws Exception {
            var result = mockMvc.perform(get("/recipes"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("recipes/index"))
                    .andReturn();

            var vm = (RecipeListViewModel) result.getModelAndView().getModel().get("vm");
            assertEquals(0L, vm.totalRecipes());
            assertEquals(0, vm.totalPages());
        }

        @Test
        void shouldPassSearchParamToModel() throws Exception {
            var result = mockMvc.perform(get("/recipes").param("search", "tarte"))
                    .andExpect(status().isOk())
                    .andReturn();

            var vm = (RecipeListViewModel) result.getModelAndView().getModel().get("vm");
            assertEquals("tarte", vm.search());
        }
    }

    @Nested
    class ViewRecipe {

        @Test
        void shouldReturnDetailViewWhenRecipeExists() throws Exception {
            var recipe = createAndSaveRecipe("Tarte aux pommes", RecipeStatus.PUBLISHED, null);

            mockMvc.perform(get("/recipes/{id}", recipe.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("recipes/detail"))
                    .andExpect(model().attributeExists("recipe"));
        }

        @Test
        void shouldReturn404WhenRecipeNotFound() throws Exception {
            var nonExistentId = UUID.randomUUID();

            mockMvc.perform(get("/recipes/{id}", nonExistentId))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldIncludeParentWhenRecipeIsVariant() throws Exception {
            var parent = createAndSaveRecipe("Recette originale", RecipeStatus.PUBLISHED, null);
            var variant = createAndSaveRecipe("Variante", RecipeStatus.PUBLISHED, parent);

            mockMvc.perform(get("/recipes/{id}", variant.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("recipes/detail"))
                    .andExpect(model().attributeExists("recipe"))
                    .andExpect(model().attributeExists("parent"));
        }

        @Test
        void shouldNotIncludeParentWhenRecipeIsNotVariant() throws Exception {
            var recipe = createAndSaveRecipe("Tarte classique", RecipeStatus.PUBLISHED, null);

            mockMvc.perform(get("/recipes/{id}", recipe.getId()))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeDoesNotExist("parent"));
        }
    }

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