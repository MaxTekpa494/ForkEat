package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.infrastructure.config.JwtFilter;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.PromotionService;
import fr.uge.forkeat.presentation.web.viewmodel.RecipeListViewModel;
import fr.uge.forkeat.service.RecipeReportService;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.RecipeSmartSearchService;
import fr.uge.forkeat.service.SmartSearchConfigService;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.model.superlike.SuperLikeConfig;
import fr.uge.forkeat.service.exception.InsufficientFundsException;
import fr.uge.forkeat.service.exception.RecipeAlreadyReportedException;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.presentation.web.viewmodel.AuthorRecipesViewModel;
import fr.uge.forkeat.service.model.recipe.RecipeReportType;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.recipe.projection.*;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.user.UserService;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
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

    @MockitoBean
    private RecipeSmartSearchService recipeSmartSearchService;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private RecipeReportService recipeReportService;

    @MockitoBean
    private SmartSearchConfigService smartSearchConfigService;

    @MockitoBean
    private PromotionService promotionService;

    private static final SuperLikeConfig DEFAULT_CONFIG = new SuperLikeConfig(
            UUID.randomUUID(), 100L, new BigDecimal("0.7"), Instant.now());

    @BeforeEach
    void setUpPromotion() {
        when(promotionService.getConfig()).thenReturn(DEFAULT_CONFIG);
        when(promotionService.findActive()).thenReturn(Optional.empty());
    }

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
            var diff = new RecipeDiff(false, "", false, "", 0, false, List.of(), List.of(), List.of(), List.of());
            var variant = new PersonalizedRecipe(
                    createRecipeWithId(variantId, "Variante", RecipeStatus.PUBLISHED, parentId),
                    RecipeCounts.ZERO, RecipeUserInteraction.NONE, diff);

            when(recipeService.findPersonalizedRecipeById(eq(variantId), any())).thenReturn(variant);

            mockMvc.perform(get("/recipes/{id}", variantId))
                    .andExpect(status().isOk())
                    .andExpect(view().name("recipes/detail"))
                    .andExpect(model().attributeExists("recipe"))
                    .andExpect(model().attributeExists("diff"));
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

    @Nested
    class SuperLikeRecipe{

        @Test
        @WithMockUser(username = "john")
        void superLikeShouldWork() throws Exception {

            var id = UUID.randomUUID();

            when(userService.getUserByUsername(any())).thenReturn(createUser());
            when(authenticationPort.extractUsername()).thenReturn(createUser().username());
            doNothing().when(recipeService).superLikeRecipe(any(), any());
            mockMvc.perform(post("/recipes/{id}/super-like", id))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(view().name("redirect:/recipes/" + id));
        }

        @Test
        @WithMockUser(username = "john")
        void superLikeShouldReturn402WhenInsufficientFunds() throws Exception {

            var id = UUID.randomUUID();

            when(userService.getUserByUsername(any())).thenReturn(createUser());
            when(authenticationPort.extractUsername()).thenReturn(createUser().username());
            doThrow(new InsufficientFundsException(50L, 100L)).when(recipeService).superLikeRecipe(any(), any());
            mockMvc.perform(post("/recipes/{id}/super-like", id))
                    .andExpect(status().isPaymentRequired());
        }
    }

    @Nested
    class MyRecipes {

        private AuthorRecipesPage buildPage(RecipeStatus status, int count) {
            var summaries = java.util.stream.IntStream.range(0, count)
                    .mapToObj(i -> {
                        var s = new RecipeSummary(UUID.randomUUID(), "Recipe " + i, "desc", null, 30, java.time.Instant.now(), "chef_test");
                        return new AuthorRecipeSummary(s, status, null);
                    })
                    .toList();
            var stats = new UserRecipeStats(count, 0, 0, 0);
            return new AuthorRecipesPage(stats, new PageResult<>(summaries, count));
        }

        @Test
        @WithMockUser
        void shouldReturnMyRecipesView() throws Exception {
            var page = buildPage(RecipeStatus.PUBLISHED, 2);
            when(authenticationPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.findRecipesByAuthor("chef_test", RecipeStatus.PUBLISHED, 0, 12)).thenReturn(page);

            mockMvc.perform(get("/recipes/my-recipes"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("recipes/my-recipes"))
                    .andExpect(model().attributeExists("vm"));
        }

        @Test
        @WithMockUser
        void shouldForwardStatusParamToService() throws Exception {
            var page = buildPage(RecipeStatus.DRAFT, 1);
            when(authenticationPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.findRecipesByAuthor("chef_test", RecipeStatus.DRAFT, 0, 12)).thenReturn(page);

            mockMvc.perform(get("/recipes/my-recipes").param("status", "DRAFT"))
                    .andExpect(status().isOk());

            verify(recipeService).findRecipesByAuthor("chef_test", RecipeStatus.DRAFT, 0, 12);
        }

        @Test
        @WithMockUser
        void shouldForwardPaginationParamsToService() throws Exception {
            var page = buildPage(RecipeStatus.PUBLISHED, 0);
            when(authenticationPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.findRecipesByAuthor("chef_test", RecipeStatus.PUBLISHED, 2, 5)).thenReturn(page);

            mockMvc.perform(get("/recipes/my-recipes").param("page", "2").param("size", "5"))
                    .andExpect(status().isOk());

            verify(recipeService).findRecipesByAuthor("chef_test", RecipeStatus.PUBLISHED, 2, 5);
        }

        @Test
        @WithMockUser
        void shouldBuildViewModelWithCorrectStats() throws Exception {
            var stats = new UserRecipeStats(3, 1, 0, 2);
            var recipes = new PageResult<AuthorRecipeSummary>(List.of(), 6L);
            var page = new AuthorRecipesPage(stats, recipes);
            when(authenticationPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.findRecipesByAuthor(any(), any(), anyInt(), anyInt())).thenReturn(page);

            var result = mockMvc.perform(get("/recipes/my-recipes"))
                    .andExpect(status().isOk())
                    .andReturn();

            var vm = (AuthorRecipesViewModel) result.getModelAndView().getModel().get("vm");
            assertEquals(3L, vm.stats().published());
            assertEquals(1L, vm.stats().draft());
            assertEquals(2L, vm.stats().rejected());
            assertEquals(6L, vm.totalRecipes());
        }

        @Test
        @WithMockUser
        void shouldCalculateTotalPagesFromResult() throws Exception {
            var stats = UserRecipeStats.ZERO;
            var recipes = new PageResult<AuthorRecipeSummary>(List.of(), 25L);
            var page = new AuthorRecipesPage(stats, recipes);
            when(authenticationPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.findRecipesByAuthor(any(), any(), anyInt(), anyInt())).thenReturn(page);

            var result = mockMvc.perform(get("/recipes/my-recipes").param("size", "10"))
                    .andExpect(status().isOk())
                    .andReturn();

            var vm = (AuthorRecipesViewModel) result.getModelAndView().getModel().get("vm");
            assertEquals(3, vm.totalPages());
        }
    }

    @Nested
    class ReportRecipe {

        @Test
        @WithMockUser(username = "john")
        void shouldRedirectWithSuccessFlashAfterReport() throws Exception {
            var id = UUID.randomUUID();
            when(authenticationPort.extractUsername()).thenReturn("john");
            when(recipeReportService.reportRecipe(any())).thenReturn(null);

            mockMvc.perform(post("/recipes/{id}/report", id)
                            .param("reportType", "SPAM")
                            .param("justification", "Ceci est du spam"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/recipes/" + id))
                    .andExpect(flash().attributeExists("reportSuccess"));
        }

        @Test
        @WithMockUser(username = "john")
        void shouldReturn404WhenRecipeNotFound() throws Exception {
            var id = UUID.randomUUID();
            when(authenticationPort.extractUsername()).thenReturn("john");
            when(recipeReportService.reportRecipe(any())).thenThrow(new RecipeNotFoundException(id));

            mockMvc.perform(post("/recipes/{id}/report", id)
                            .param("reportType", "SPAM")
                            .param("justification", "Recette introuvable"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "john")
        void shouldRedirectWithErrorFlashWhenAlreadyReported() throws Exception {
            var id = UUID.randomUUID();
            when(authenticationPort.extractUsername()).thenReturn("john");
            when(recipeReportService.reportRecipe(any()))
                    .thenThrow(new RecipeAlreadyReportedException(id, UUID.randomUUID()));

            mockMvc.perform(post("/recipes/{id}/report", id)
                            .param("reportType", "SPAM")
                            .param("justification", "Déjà signalé"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("reportError"));
        }
    }

    @Nested
    class ListRecipesFollowingTest {

        private RecipeListViewModel getViewModel(MvcResult result) {
            return (RecipeListViewModel) result.getModelAndView().getModel().get("vm");
        }

        private void givenServiceReturns(List<PersonalizedRecipeSummary> items, long total) {
            when(recipeService.getPersonalizedFeedRecipes(any(), anyInt()))
                    .thenReturn(new PageResult<>(items, total));
            when(recipeService.findAllAllergens()).thenReturn(List.of());
        }

        @Nested
        class WhenSessionHasNoInstant {

            @Test
            void shouldCreateInstantInSession() throws Exception {
                givenServiceReturns(List.of(), 0L);

                mockMvc.perform(get("/recipes/following"))
                        .andExpect(status().isOk())
                        .andExpect(request().sessionAttribute("instant", instanceOf(Instant.class)));
            }

            @Test
            void shouldCallServiceWithAnyInstant() throws Exception {
                givenServiceReturns(List.of(), 0L);

                mockMvc.perform(get("/recipes/following"))
                        .andExpect(status().isOk());

                verify(recipeService).getPersonalizedFeedRecipes(any(), eq(0));
            }
        }

        @Nested
        class WhenSessionAlreadyHasInstant {

            @Test
            void shouldReuseExistingInstant() throws Exception {
                var fixedInstant = Instant.parse("2024-01-15T10:00:00Z");
                givenServiceReturns(List.of(), 0L);

                mockMvc.perform(get("/recipes/following")
                                .sessionAttr("instant", fixedInstant))
                        .andExpect(status().isOk());

                verify(recipeService).getPersonalizedFeedRecipes(eq(fixedInstant), eq(0));
            }

            @Test
            void shouldNotOverwriteExistingInstant() throws Exception {
                var fixedInstant = Instant.parse("2024-01-15T10:00:00Z");
                givenServiceReturns(List.of(), 0L);

                mockMvc.perform(get("/following")
                                .sessionAttr("instant", fixedInstant))
                        .andExpect(request().sessionAttribute("instant", fixedInstant));
            }
        }

        @Nested
        class WhenSessionHasInvalidInstant {

            @Test
            void shouldFallbackToNewInstantWhenAttributeIsNotAnInstant() throws Exception {
                givenServiceReturns(List.of(), 0L);

                mockMvc.perform(get("/recipes/following")
                                .sessionAttr("instant", "not-an-instant"))
                        .andExpect(status().isOk());

                verify(recipeService).getPersonalizedFeedRecipes(any(), eq(0));
            }
        }

        @Nested
        class ViewModel {

            @Test
            void shouldSetIsFollowingToTrue() throws Exception {
                givenServiceReturns(List.of(), 0L);

                mockMvc.perform(get("/recipes/following"))
                        .andExpect(model().attribute("isFollowing", true));
            }

            @Test
            void shouldReturnCorrectView() throws Exception {
                givenServiceReturns(List.of(), 0L);

                mockMvc.perform(get("/recipes/following"))
                        .andExpect(view().name("recipes/index"));
            }

            @Test
            void shouldComputeTotalPagesCorrectly() throws Exception {
                givenServiceReturns(List.of(), 41L); // 41 / 20 = ceil(2.05) = 3 pages

                var result = mockMvc.perform(get("/recipes/following"))
                        .andExpect(status().isOk()).andReturn();
                assertEquals(3, getViewModel(result).totalPages());
            }

            @Test
            void shouldPassPageParamToViewModel() throws Exception {
                givenServiceReturns(List.of(), 0L);

                var result = mockMvc.perform(get("/recipes/following").param("page", "2"))
                        .andExpect(status().isOk()).andReturn();
                assertEquals(2, getViewModel(result).currentPage());

            }

            @Test
            void shouldDefaultToPageZeroWhenParamAbsent() throws Exception {
                givenServiceReturns(List.of(), 0L);

                var result = mockMvc.perform(get("/recipes/following"))
                        .andExpect(status().isOk()).andReturn();
                assertEquals(0, getViewModel(result).currentPage());

            }

            @Test
            void shouldPassTotalToViewModel() throws Exception {
                givenServiceReturns(List.of(), 42L);

                var result = mockMvc.perform(get("/recipes/following")).andExpect(status().isOk()).andReturn();
                assertEquals(3, getViewModel(result).totalPages());
            }

            @Test
            void shouldMapRecipesToDTOs() throws Exception {
                var summary = aPersonalizedSummary();
                givenServiceReturns(List.of(summary), 1L);

                var result = mockMvc.perform(get("/recipes/following"))
                        .andExpect(status().isOk()).andReturn();
                assertEquals(1, getViewModel(result).recipes().size());
            }
        }

        // --- helpers ---

        private PersonalizedRecipeSummary aPersonalizedSummary() {
            var recipe = new RecipeSummary(UUID.randomUUID(), "Pasta", "", "", 0, Instant.now(), "");
            return new PersonalizedRecipeSummary(recipe, RecipeCounts.ZERO, RecipeUserInteraction.NONE);
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