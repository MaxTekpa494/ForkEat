package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.config.JwtFilter;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipe;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipeSummary;
import fr.uge.forkeat.service.model.recipe.projection.RecipeCounts;
import fr.uge.forkeat.service.model.recipe.projection.RecipeSummary;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.user.UserService;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecipeWebController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecipeWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private AuthenticationPort authenticationPort;

    @Nested
    class ListRecipes {

        @Test
        @WithMockUser
        void shouldReturnIndexViewWithDefaultParams() throws Exception {
            var recipe = createPersonalizedSummary("Tarte aux pommes");
            var pageResult = new PageResult<>(List.of(recipe), 1L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, null, List.of(), 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);
            when(recipeService.findAllAllergens()).thenReturn(List.of());

            mockMvc.perform(get("/recipes"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("recipes/index"))
                    .andExpect(model().attributeExists("vm"));
        }

        @Test
        @WithMockUser
        void shouldPassCustomStatusAndPagination() throws Exception {
            var recipe = createPersonalizedSummary("Brouillon");
            var pageResult = new PageResult<>(List.of(recipe), 1L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.DRAFT, null, List.of(), 5, 2);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);
            when(recipeService.findAllAllergens()).thenReturn(List.of());

            mockMvc.perform(get("/recipes")
                            .param("status", "DRAFT")
                            .param("size", "5")
                            .param("page", "2"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("recipes/index"));
        }

        @Test
        @WithMockUser
        void shouldCalculateTotalPagesCorrectly() throws Exception {
            var recipes = List.of(
                    createPersonalizedSummary("Recipe 1"),
                    createPersonalizedSummary("Recipe 2")
            );
            var pageResult = new PageResult<>(recipes, 5L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, null, List.of(), 2, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);
            when(recipeService.findAllAllergens()).thenReturn(List.of());

            mockMvc.perform(get("/recipes")
                            .param("size", "2"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        void shouldReturnEmptyListWhenNoRecipes() throws Exception {
            var pageResult = new PageResult<>(List.<PersonalizedRecipeSummary>of(), 0L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, null, List.of(), 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);
            when(recipeService.findAllAllergens()).thenReturn(List.of());

            mockMvc.perform(get("/recipes"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("recipes/index"));
        }
    }

    @Nested
    class ListRecipesWithSearch {

        @Test
        @WithMockUser
        void shouldPassSearchParam() throws Exception {
            var pageResult = new PageResult<>(List.<PersonalizedRecipeSummary>of(), 0L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, "tarte", List.of(), 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);
            when(recipeService.findAllAllergens()).thenReturn(List.of());

            mockMvc.perform(get("/recipes")
                            .param("search", "tarte"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("recipes/index"));

            verify(recipeService).searchRecipes(criteria);
        }
    }

    @Nested
    class ListRecipesWithAllergens {

        @Test
        @WithMockUser
        void shouldUseSearchAndAllergens_whenBothProvided() throws Exception {
            var recipe = createPersonalizedSummary("Salade verte");
            var pageResult = new PageResult<>(List.of(recipe), 1L);
            var allergens = List.of("Gluten", "Lactose");
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, "salade", allergens, 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);
            when(recipeService.findAllAllergens()).thenReturn(List.of());

            mockMvc.perform(get("/recipes")
                            .param("search", "salade")
                            .param("allergens", "Gluten", "Lactose"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("recipes/index"));

            verify(recipeService).searchRecipes(criteria);
        }

        @Test
        @WithMockUser
        void shouldUseAllergens_whenOnlyAllergensProvided() throws Exception {
            var pageResult = new PageResult<>(List.<PersonalizedRecipeSummary>of(), 0L);
            var allergens = List.of("Gluten");
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, null, allergens, 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);
            when(recipeService.findAllAllergens()).thenReturn(List.of());

            mockMvc.perform(get("/recipes")
                            .param("allergens", "Gluten"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("recipes/index"));

            verify(recipeService).searchRecipes(criteria);
        }

        @Test
        @WithMockUser
        void shouldPassAllAllergensToModel() throws Exception {
            var pageResult = new PageResult<>(List.<PersonalizedRecipeSummary>of(), 0L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, null, List.of(), 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            var allergensList = List.of(
                    new Allergen(UUID.randomUUID(), "Gluten", AllergenSeverity.HIGH),
                    new Allergen(UUID.randomUUID(), "Lactose", AllergenSeverity.MEDIUM)
            );
            when(recipeService.findAllAllergens()).thenReturn(allergensList);

            mockMvc.perform(get("/recipes"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("vm"));
        }
    }

    @Nested
    class ViewRecipe {

        @Test
        @WithMockUser
        void shouldReturnDetailViewWhenRecipeExists() throws Exception {
            var id = UUID.randomUUID();
            var recipe = createRecipeWithMetaDataWithId(id, "Quiche Lorraine", RecipeStatus.PUBLISHED, null);

            when(recipeService.findPersonalizedRecipeById(eq(id), any())).thenReturn(recipe);

            mockMvc.perform(get("/recipes/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(view().name("recipes/detail"))
                    .andExpect(model().attributeExists("recipe"));
        }

        @Test
        @WithMockUser
        void shouldIncludeParentWhenRecipeIsVariant() throws Exception {
            var parentId = UUID.randomUUID();
            var variantId = UUID.randomUUID();
            var parent = createRecipeWithId(parentId, "Recette originale", RecipeStatus.PUBLISHED, null);
            var variant = createRecipeWithMetaDataWithId(variantId, "Variante", RecipeStatus.PUBLISHED, parentId);

            when(recipeService.findPersonalizedRecipeById(eq(variantId), any())).thenReturn(variant);
            when(recipeService.findById(parentId)).thenReturn(parent);

            mockMvc.perform(get("/recipes/{id}", variantId))
                    .andExpect(status().isOk())
                    .andExpect(view().name("recipes/detail"))
                    .andExpect(model().attributeExists("recipe"))
                    .andExpect(model().attributeExists("parent"));
        }

        @Test
        @WithMockUser
        void shouldNotIncludeParentWhenRecipeIsNotVariant() throws Exception {
            var id = UUID.randomUUID();
            var recipe = createRecipeWithMetaDataWithId(id, "Tarte classique", RecipeStatus.PUBLISHED, null);

            when(recipeService.findPersonalizedRecipeById(eq(id), any())).thenReturn(recipe);

            mockMvc.perform(get("/recipes/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeDoesNotExist("parent"));
        }

        @Test
        @WithMockUser
        void shouldReturn404WhenRecipeNotFound() throws Exception {
            var id = UUID.randomUUID();

            when(recipeService.findPersonalizedRecipeById(eq(id), any())).thenThrow(new RecipeNotFoundException(id));

            mockMvc.perform(get("/recipes/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class likeRecipe{

        @Test
        @WithMockUser(username = "john")
        void likeShouldWork() throws Exception {

            var id = UUID.randomUUID();


            when(userService.getUserByUsername(any())).thenReturn(createUser());
            when(authenticationPort.extractUsername()).thenReturn(createUser().username());
            doNothing().when(recipeService).likeRecipe(any(), any());
            mockMvc.perform(post("/recipes/{id}/like", id))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(view().name("redirect:/recipes/" + id));


        }

        @Test
        @WithMockUser(username = "john")
        void unlikeShouldWork() throws Exception {

            var id = UUID.randomUUID();


            when(userService.getUserByUsername(any())).thenReturn(createUser());
            when(authenticationPort.extractUsername()).thenReturn(createUser().username());
            doNothing().when(recipeService).unlikeRecipe(any(), any());
            mockMvc.perform(post("/recipes/{id}/unlike", id))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(view().name("redirect:/recipes/" + id));


        }
    }

    private Recipe createRecipe(String title, RecipeStatus status) {
        return createRecipeWithId(UUID.randomUUID(), title, status, null);
    }

    private User createUser() {
        return new User(UUID.randomUUID(), "Pax", "Maximus", "Prime", "aaa@aa.fr", UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, Instant.now(), Instant.now(), true);
    }

    private Recipe createRecipeWithId(UUID id, String title, RecipeStatus status, UUID parentId) {
        return new Recipe(
                id,
                title,
                "Summary for " + title,
                parentId,
                "chef_test",
                30,
                null,
                status,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                null
        );
    }

    private PersonalizedRecipe createRecipeWithMetaDataWithId(UUID id, String title, RecipeStatus status, UUID parentId) {
        return new PersonalizedRecipe(
                createRecipeWithId(id, title, status, parentId),
                RecipeCounts.ZERO,
                RecipeUserInteraction.NONE
        );
    }

    private PersonalizedRecipeSummary createPersonalizedSummary(String title) {
        var s = new RecipeSummary(UUID.randomUUID(), title, "Summary for " + title, null, 30, Instant.now(), "chef_test");
        return new PersonalizedRecipeSummary(s, RecipeCounts.ZERO, RecipeUserInteraction.NONE);
    }
}