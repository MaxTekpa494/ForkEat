package fr.uge.forkeat.presentation.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.uge.forkeat.presentation.rest.GlobalRestExceptionHandler;
import org.springframework.context.annotation.Import;
import fr.uge.forkeat.presentation.dto.recipe.CreateRecipeRequest;
import fr.uge.forkeat.presentation.dto.recipe.UpdateRecipeRequest;
import fr.uge.forkeat.presentation.dto.recipe.RecipeReportRequestDTO;
import fr.uge.forkeat.presentation.dto.recipe.RecipePaginationDTO;
import fr.uge.forkeat.presentation.dto.recipe.RecipeSearchDTO;
import fr.uge.forkeat.presentation.response.CreatedResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.presentation.response.NotContentResponse;
import fr.uge.forkeat.service.RecipeReportService;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.*;
import fr.uge.forkeat.service.exception.*;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.recipe.projection.*;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipe;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipeSummary;
import fr.uge.forkeat.service.model.recipe.projection.RecipeCounts;
import fr.uge.forkeat.service.model.recipe.projection.RecipeSummary;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.security.SecurityService;
import fr.uge.forkeat.service.user.UserService;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeSearchCriteria;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeRestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalRestExceptionHandler.class)
class RecipeRestControllerTest {

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private RecipeReportService recipeReportService;

    @MockitoBean
    private RecipeSmartSearchService recipeSmartSearchService;

    @MockitoBean
    private SmartSearchConfigService smartSearchConfigService;

    @MockitoBean
    private SecurityService securityService;

    @MockitoBean
    private WalletService walletService;

    private RecipeRestController recipeController;
    private Instant now;
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    RecipeRestControllerTest(MockMvc mockMvc){this.mockMvc = mockMvc;}

    @BeforeEach
    void setUp() {
        recipeController = new RecipeRestController(recipeService, recipeReportService, authPort, userService,
                recipeSmartSearchService, smartSearchConfigService);
        now = Instant.now();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========== CreateRecipe ==========

    @Nested
    class CreateRecipe {

        @Test
        void shouldCreateRecipeWithAuthenticatedUsername() {
            var recipeId = UUID.randomUUID();
            var savedRecipe = createRecipe(recipeId, "Tarte aux pommes", null, RecipeStatus.DRAFT);
            var request = new CreateRecipeRequest(null, "Tarte aux pommes", "Une bonne tarte", 30, true, null, List.of(), List.of(), List.of(), List.of());

            when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.createRecipe(any(CreateRecipeCommand.class))).thenReturn(savedRecipe);

            var response = recipeController.createRecipe(request, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(CreatedResponse.class, response.getBody());
            verify(authPort).extractUsername();
            verify(recipeService).createRecipe(any(CreateRecipeCommand.class));
        }

        @Test
        void shouldUseAuthenticatedUsernameNotDtoUsername() {
            var recipeId = UUID.randomUUID();
            var savedRecipe = createRecipe(recipeId, "Recette", null, RecipeStatus.DRAFT);
            var request = new CreateRecipeRequest(null, "Recette", "Résumé", 20, true, null, List.of(), List.of(), List.of(), List.of());

            when(authPort.extractUsername()).thenReturn("real_author");
            when(recipeService.createRecipe(any(CreateRecipeCommand.class))).thenReturn(savedRecipe);

            recipeController.createRecipe(request, null);

            verify(authPort).extractUsername();
        }

        @Test
        void shouldThrowWhenDtoIsNull() {
            assertThrows(NullPointerException.class, () -> recipeController.createRecipe(null, null));
        }
    }

    // ========== GetRecipe ==========

    @Nested
    class GetRecipe {

        @Test
        void shouldReturnRecipeWhenFound() {
            var recipeId = UUID.randomUUID();
            var recipe = createRecipeWithMetadata(recipeId, "Tarte aux pommes", null, RecipeStatus.PUBLISHED);
            when(recipeService.findPersonalizedRecipeById(eq(recipeId), any())).thenReturn(recipe);

            var response = recipeController.getRecipe(recipeId);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(ItemResponse.class, response.getBody());
            var itemResponse = (ItemResponse<?>) response.getBody();
            assertNotNull(itemResponse.resource());
            verify(recipeService).findPersonalizedRecipeById(eq(recipeId), any());
        }

        @Test
        void shouldReturnRecipeWithParentIdWhenVariant() {
            var parentId = UUID.randomUUID();
            var childId = UUID.randomUUID();
            var childRecipe = createRecipeWithMetadata(childId, "Variante", parentId, RecipeStatus.PUBLISHED);

            when(recipeService.findPersonalizedRecipeById(eq(childId), any())).thenReturn(childRecipe);

            var response = recipeController.getRecipe(childId);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            var itemResponse = (ItemResponse<?>) response.getBody();
            assertNotNull(itemResponse);
            verify(recipeService).findPersonalizedRecipeById(eq(childId), any());
            verifyNoMoreInteractions(recipeService);
        }

        @Test
        void shouldNotFetchParentWhenNotVariant() {
            var recipeId = UUID.randomUUID();
            var recipe = createRecipeWithMetadata(recipeId, "Recette simple", null, RecipeStatus.PUBLISHED);
            when(recipeService.findPersonalizedRecipeById(eq(recipeId), any())).thenReturn(recipe);

            recipeController.getRecipe(recipeId);

            verify(recipeService, times(1)).findPersonalizedRecipeById(eq(recipeId), any());
            verifyNoMoreInteractions(recipeService);
        }

        @Test
        void shouldPropagateExceptionWhenNotFound() {
            var recipeId = UUID.randomUUID();
            when(recipeService.findPersonalizedRecipeById(eq(recipeId), any())).thenThrow(new RecipeNotFoundException(recipeId));

            assertThrows(RecipeNotFoundException.class, () -> recipeController.getRecipe(recipeId));
            verify(recipeService).findPersonalizedRecipeById(eq(recipeId), any());
        }

        @Test
        void shouldThrow404WhenRecipeNotPublishedAndUserIsAnonymous() {
            // Le guard est dans le service : on vérifie que le contrôleur passe bien null
            // pour un utilisateur anonyme, et propage l'exception levée par le service
            var recipeId = UUID.randomUUID();
            when(recipeService.findPersonalizedRecipeById(eq(recipeId), isNull()))
                    .thenThrow(new RecipeNotFoundException(recipeId));

            assertThrows(RecipeNotFoundException.class, () -> recipeController.getRecipe(recipeId));
            verify(recipeService).findPersonalizedRecipeById(eq(recipeId), isNull());
        }

        @Test
        void shouldReturnDraftRecipeWhenCurrentUserIsAuthor() {
            // Simuler un utilisateur authentifié via SecurityContextHolder
            var auth = new UsernamePasswordAuthenticationToken("chef_test", null, List.of());
            var ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(auth);
            SecurityContextHolder.setContext(ctx);

            var recipeId = UUID.randomUUID();
            // La recette appartient à "chef_test" (défini dans createRecipe)
            var recipe = createRecipeWithMetadata(recipeId, "Mon brouillon", null, RecipeStatus.DRAFT);
            when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.findPersonalizedRecipeById(eq(recipeId), eq("chef_test"))).thenReturn(recipe);

            var response = recipeController.getRecipe(recipeId);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(recipeService).findPersonalizedRecipeById(eq(recipeId), eq("chef_test"));
        }

        @Test
        void shouldThrow404WhenDraftIsAccessedByAnotherUser() {
            var auth = new UsernamePasswordAuthenticationToken("other_user", null, List.of());
            var ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(auth);
            SecurityContextHolder.setContext(ctx);

            var recipeId = UUID.randomUUID();
            when(authPort.extractUsername()).thenReturn("other_user");
            when(recipeService.findPersonalizedRecipeById(eq(recipeId), eq("other_user")))
                    .thenThrow(new RecipeNotFoundException(recipeId));

            assertThrows(RecipeNotFoundException.class, () -> recipeController.getRecipe(recipeId));
            verify(recipeService).findPersonalizedRecipeById(eq(recipeId), eq("other_user"));
        }
    }

    // ========== GetAllergens ==========

    @Nested
    class GetAllergens {

        @Test
        void shouldReturnAllAllergens() {
            var allergen1 = new Allergen(UUID.randomUUID(), "Gluten", AllergenSeverity.HIGH);
            var allergen2 = new Allergen(UUID.randomUUID(), "Lactose", AllergenSeverity.MEDIUM);
            when(recipeService.findAllAllergens()).thenReturn(List.of(allergen1, allergen2));

            var response = recipeController.getAllergens();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(ListResponse.class, response.getBody());
            var listResponse = (ListResponse<?>) response.getBody();
            assertEquals(2, listResponse.resources().size());
            assertEquals(2, listResponse.total());
            verify(recipeService).findAllAllergens();
        }

        @Test
        void shouldReturnEmptyListWhenNoAllergens() {
            when(recipeService.findAllAllergens()).thenReturn(List.of());

            var response = recipeController.getAllergens();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            var listResponse = (ListResponse<?>) response.getBody();
            assertNotNull(listResponse);
            assertTrue(listResponse.resources().isEmpty());
            assertEquals(0, listResponse.total());
            verify(recipeService).findAllAllergens();
        }
    }

    // ========== PageCreateRecipe ==========

    @Nested
    class PageCreateRecipe {

        @Test
        void shouldReturnAllergensIngredientsAndDietaries() {
            var allergen = new Allergen(UUID.randomUUID(), "Gluten", AllergenSeverity.HIGH);
            when(recipeService.findAllAllergens()).thenReturn(List.of(allergen));
            when(recipeService.findAllIngredientNames()).thenReturn(List.of("Pomme", "Farine", "Beurre"));
            when(recipeService.findAllDietaryNames()).thenReturn(List.of("vegan", "végétarien"));

            var response = recipeController.pageCreateRecipe();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(ItemResponse.class, response.getBody());
            var data = (RecipeRestController.AllergensIngredients) ((ItemResponse<?>) response.getBody()).resource();
            assertEquals(1, data.allergens().size());
            assertEquals(3, data.ingredients().size());
            assertEquals(2, data.dietaries().size());
            verify(recipeService).findAllAllergens();
            verify(recipeService).findAllIngredientNames();
            verify(recipeService).findAllDietaryNames();
        }

        @Test
        void shouldReturnEmptyListsWhenNoneAvailable() {
            when(recipeService.findAllAllergens()).thenReturn(List.of());
            when(recipeService.findAllIngredientNames()).thenReturn(List.of());
            when(recipeService.findAllDietaryNames()).thenReturn(List.of());

            var response = recipeController.pageCreateRecipe();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assert response.getBody() != null;
            var data = (RecipeRestController.AllergensIngredients) ((ItemResponse<?>) response.getBody()).resource();
            assertTrue(data.allergens().isEmpty());
            assertTrue(data.ingredients().isEmpty());
            assertTrue(data.dietaries().isEmpty());
        }
    }

    // ========== GetRecipes ==========

    @Nested
    class GetRecipes {

        @Test
        void shouldReturnPaginatedList() {
            var ps1 = createPersonalizedRecipeSummary(UUID.randomUUID(), "Recette 1");
            var ps2 = createPersonalizedRecipeSummary(UUID.randomUUID(), "Recette 2");
            var pageResult = new PageResult<>(List.of(ps1, ps2), 10L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, null, List.of(), 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            var response = recipeController.getRecipes(new RecipeSearchDTO("PUBLISHED", 12, 0, null, null));

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(ListResponse.class, response.getBody());
            var listResponse = (ListResponse<?>) response.getBody();
            assertEquals(2, listResponse.resources().size());
            assertEquals(10, listResponse.total());
        }

        @Test
        void shouldReturnEmptyListWhenNoRecipes() {
            var pageResult = new PageResult<PersonalizedRecipeSummary>(List.of(), 0L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.DRAFT, null, List.of(), 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            var response = recipeController.getRecipes(new RecipeSearchDTO("DRAFT", 12, 0, null, null));

            assertEquals(HttpStatus.OK, response.getStatusCode());
            var listResponse = (ListResponse<?>) response.getBody();
            assert listResponse != null;
            assertTrue(listResponse.resources().isEmpty());
            assertEquals(0, listResponse.total());
        }

        @Test
        void shouldRespectSizeParameter() {
            var ps = createPersonalizedRecipeSummary(UUID.randomUUID(), "Recette");
            var pageResult = new PageResult<>(List.of(ps), 100L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, null, List.of(), 5, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            recipeController.getRecipes(new RecipeSearchDTO("PUBLISHED", 5, 0, null, null));

            verify(recipeService).searchRecipes(criteria);
        }

        @Test
        void shouldRespectPageParameter() {
            var ps = createPersonalizedRecipeSummary(UUID.randomUUID(), "Recette");
            var pageResult = new PageResult<>(List.of(ps), 100L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, null, List.of(), 12, 2);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            recipeController.getRecipes(new RecipeSearchDTO("PUBLISHED", 12, 2, null, null));

            verify(recipeService).searchRecipes(criteria);
        }

        @Test
        void shouldFilterByStatus() {
            var ps = createPersonalizedRecipeSummary(UUID.randomUUID(), "Brouillon");
            var pageResult = new PageResult<>(List.of(ps), 1L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.DRAFT, null, List.of(), 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            var response = recipeController.getRecipes(new RecipeSearchDTO("DRAFT", 12, 0, null, null));

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        void shouldHandlePendingReviewStatus() {
            var ps = createPersonalizedRecipeSummary(UUID.randomUUID(), "En attente");
            var pageResult = new PageResult<>(List.of(ps), 1L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PENDING_REVIEW, null, List.of(), 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            var response = recipeController.getRecipes(new RecipeSearchDTO("PENDING_REVIEW", 12, 0, null, null));

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ========== GetRecipesWithSearch ==========

    @Nested
    class GetRecipesWithSearch {

        @Test
        void shouldSearchWhenSearchParameterProvided() {
            var ps = createPersonalizedRecipeSummary(UUID.randomUUID(), "Tarte aux pommes");
            var pageResult = new PageResult<>(List.of(ps), 1L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, "tarte", List.of(), 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            var response = recipeController.getRecipes(new RecipeSearchDTO("PUBLISHED", 12, 0, "tarte", null));

            assertEquals(HttpStatus.OK, response.getStatusCode());
            var listResponse = (ListResponse<?>) response.getBody();
            assert listResponse != null;
            assertEquals(1, listResponse.resources().size());
            verify(recipeService).searchRecipes(criteria);
            verifyNoMoreInteractions(recipeService);
        }
    }

    // ========== GetRecipesWithAllergens ==========

    @Nested
    class GetRecipesWithAllergens {

        @Test
        void shouldFilterByAllergensWhenProvided() {
            var ps = createPersonalizedRecipeSummary(UUID.randomUUID(), "Recette sans gluten");
            var allergens = List.of("gluten", "lactose");
            var pageResult = new PageResult<>(List.of(ps), 1L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, null, allergens, 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            var response = recipeController.getRecipes(new RecipeSearchDTO("PUBLISHED", 12, 0, null, allergens));

            assertEquals(HttpStatus.OK, response.getStatusCode());
            var listResponse = (ListResponse<?>) response.getBody();
            assert listResponse != null;
            assertEquals(1, listResponse.resources().size());
            verify(recipeService).searchRecipes(criteria);
            verifyNoMoreInteractions(recipeService);
        }

        @Test
        void shouldFilterByAllergensAndSearchWhenBothProvided() {
            var ps = createPersonalizedRecipeSummary(UUID.randomUUID(), "Tarte sans gluten");
            var allergens = List.of("gluten");
            var pageResult = new PageResult<>(List.of(ps), 1L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, "tarte", allergens, 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            var response = recipeController.getRecipes(new RecipeSearchDTO("PUBLISHED", 12, 0, "tarte", allergens));

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(recipeService).searchRecipes(criteria);
            verifyNoMoreInteractions(recipeService);
        }

        @Test
        void shouldWorkWithEmptyAllergensList() {
            var ps = createPersonalizedRecipeSummary(UUID.randomUUID(), "Recette");
            var pageResult = new PageResult<>(List.of(ps), 1L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, null, List.of(), 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            var response = recipeController.getRecipes(new RecipeSearchDTO("PUBLISHED", 12, 0, null, List.of()));

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(recipeService).searchRecipes(criteria);
        }
    }

    // ========== UpdateRecipe ==========

    @Nested
    class UpdateRecipe {

        @Test
        void shouldUpdateRecipeWhenUserIsOwner() {
            var recipeId = UUID.randomUUID();
            var updated = createRecipe(recipeId, "Nouveau titre", null, RecipeStatus.PENDING_REVIEW);
            var request = new UpdateRecipeRequest("Nouveau titre", "Résumé", 30, false, null, List.of(), List.of(), List.of(), List.of());

            when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.updateRecipe(any(UpdateRecipeCommand.class))).thenReturn(updated);

            var response = recipeController.updateRecipe(recipeId, request, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(ItemResponse.class, response.getBody());
            verify(recipeService).updateRecipe(any(UpdateRecipeCommand.class));
        }

        @Test
        void shouldThrowWhenUserIsNotOwner() {
            var recipeId = UUID.randomUUID();
            var request = new UpdateRecipeRequest("Nouveau titre", "Résumé", 30, false, null, List.of(), List.of(), List.of(), List.of());

            when(authPort.extractUsername()).thenReturn("intruder");
            when(recipeService.updateRecipe(any(UpdateRecipeCommand.class))).thenThrow(new RecipeOwnershipException(UUID.randomUUID(), "a"));

            assertThrows(RecipeOwnershipException.class, () -> recipeController.updateRecipe(recipeId, request, null));
        }

        @Test
        void shouldPropagateExceptionWhenRecipeNotFound() {
            var recipeId = UUID.randomUUID();
            var request = new UpdateRecipeRequest("Titre", "Résumé", 30, false, null, List.of(), List.of(), List.of(), List.of());

            when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.updateRecipe(any(UpdateRecipeCommand.class))).thenThrow(new RecipeNotFoundException(recipeId));

            assertThrows(RecipeNotFoundException.class, () -> recipeController.updateRecipe(recipeId, request, null));
        }

        @Test
        void shouldThrowWhenRequestIsNull() {
            assertThrows(NullPointerException.class, () -> recipeController.updateRecipe(UUID.randomUUID(), null, null));
        }

        @Test
        void shouldUpdateRecipeWithImage() {
            var recipeId = UUID.randomUUID();
            var updated = createRecipe(recipeId, "Recette", null, RecipeStatus.PENDING_REVIEW);
            var request = new UpdateRecipeRequest("Recette", "Résumé", 30, false, null, List.of(), List.of(), List.of(), List.of());
            var mockImage = mock(MultipartFile.class);

            when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.updateRecipe(any(UpdateRecipeCommand.class))).thenReturn(updated);

            var response = recipeController.updateRecipe(recipeId, request, mockImage);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(recipeService).updateRecipe(any(UpdateRecipeCommand.class));
        }
    }

    // ========== DeleteRecipe ==========

    @Nested
    class DeleteRecipe {

        @Test
        void shouldDeleteRecipeWhenUserIsOwner() {
            var recipeId = UUID.randomUUID();
            var existing = createRecipe(recipeId, "Recette à supprimer", null, RecipeStatus.PUBLISHED);

            when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.findById(recipeId)).thenReturn(existing);
            doNothing().when(recipeService).deleteById(recipeId);

            var response = recipeController.deleteRecipe(recipeId);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(NotContentResponse.class, response.getBody());
            verify(recipeService).deleteById(recipeId);
        }

        @Test
        void shouldThrowWhenUserIsNotOwner() {
            var recipeId = UUID.randomUUID();
            var existing = createRecipe(recipeId, "Recette de chef_test", null, RecipeStatus.PUBLISHED);

            when(authPort.extractUsername()).thenReturn("intruder");
            doThrow(new RecipeOwnershipException(UUID.randomUUID(), "a")).when(recipeService).deleteById(recipeId);

            assertThrows(RecipeOwnershipException.class, () -> recipeController.deleteRecipe(recipeId));
        }

        @Test
        void shouldPropagateExceptionWhenRecipeNotFound() {
            var recipeId = UUID.randomUUID();

            when(authPort.extractUsername()).thenReturn("chef_test");
            doThrow(new RecipeNotFoundException(recipeId)).when(recipeService).deleteById(any());

            assertThrows(RecipeNotFoundException.class, () -> recipeController.deleteRecipe(recipeId));
        }
    }

    // ========== MyRecipes ==========

    @Nested
    class MyRecipes {

        private RecipePaginationDTO defaultPagination() {
            var p = new RecipePaginationDTO();
            p.setStatus("PUBLISHED");
            p.setPage(0);
            p.setSize(10);
            return p;
        }

        @Test
        void shouldReturnRecipesForAuthenticatedUser() {
            var summary1 = new RecipeSummary(UUID.randomUUID(), "Ma recette 1", "desc", null, 30, null, "chef_test");
            var summary2 = new RecipeSummary(UUID.randomUUID(), "Ma recette 2", "desc", null, 15, null, "chef_test");
            var authorSummary1 = new AuthorRecipeSummary(summary1, RecipeStatus.PUBLISHED, null);
            var authorSummary2 = new AuthorRecipeSummary(summary2, RecipeStatus.PUBLISHED, null);
            var stats = new UserRecipeStats(2, 0, 0, 0);
            var page = new PageResult<>(List.of(authorSummary1, authorSummary2), 2L);
            var authorRecipesPage = new AuthorRecipesPage(stats, page);

            when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.findRecipesByAuthor("chef_test", RecipeStatus.PUBLISHED, 0, 10)).thenReturn(authorRecipesPage);

            var response = recipeController.myRecipes(defaultPagination());

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(ItemResponse.class, response.getBody());
            var itemResponse = (ItemResponse<?>) response.getBody();
            assertInstanceOf(AuthorRecipesPage.class, itemResponse.resource());
            var result = (AuthorRecipesPage) itemResponse.resource();
            assertEquals(2, result.recipes().items().size());
            assertEquals(2, result.recipes().total());
            assertEquals(2, result.stats().published());
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoRecipes() {
            var stats = new UserRecipeStats(0, 0, 0, 0);
            var page = new PageResult<AuthorRecipeSummary>(List.of(), 0L);
            var authorRecipesPage = new AuthorRecipesPage(stats, page);

            when(authPort.extractUsername()).thenReturn("nouveau_chef");
            when(recipeService.findRecipesByAuthor("nouveau_chef", RecipeStatus.PUBLISHED, 0, 10)).thenReturn(authorRecipesPage);

            var response = recipeController.myRecipes(defaultPagination());

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(ItemResponse.class, response.getBody());
            var result = (AuthorRecipesPage) ((ItemResponse<?>) response.getBody()).resource();
            assertTrue(result.recipes().items().isEmpty());
            assertEquals(0, result.recipes().total());
        }

        @Test
        void shouldUseAuthenticatedUsernameToFetchRecipes() {
            var stats = UserRecipeStats.ZERO;
            var authorRecipesPage = new AuthorRecipesPage(stats, new PageResult<>(List.of(), 0L));

            when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.findRecipesByAuthor("chef_test", RecipeStatus.PUBLISHED, 0, 10)).thenReturn(authorRecipesPage);

            recipeController.myRecipes(defaultPagination());

            verify(authPort).extractUsername();
            verify(recipeService).findRecipesByAuthor("chef_test", RecipeStatus.PUBLISHED, 0, 10);
        }

        @Test
        void shouldForwardStatusParameterToService() {
            var stats = new UserRecipeStats(0, 2, 0, 0);
            var summary = new RecipeSummary(UUID.randomUUID(), "Brouillon", "desc", null, 10, null, "chef_test");
            var authorSummary = new AuthorRecipeSummary(summary, RecipeStatus.DRAFT, null);
            var page = new PageResult<>(List.of(authorSummary), 1L);
            var authorRecipesPage = new AuthorRecipesPage(stats, page);

            when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.findRecipesByAuthor("chef_test", RecipeStatus.DRAFT, 0, 10)).thenReturn(authorRecipesPage);

            var pagination = new RecipePaginationDTO();
            pagination.setStatus("DRAFT");
            pagination.setPage(0);
            pagination.setSize(10);

            var response = recipeController.myRecipes(pagination);

            verify(recipeService).findRecipesByAuthor("chef_test", RecipeStatus.DRAFT, 0, 10);
          assert response.getBody() != null;
          var result = (AuthorRecipesPage) ((ItemResponse<?>) response.getBody()).resource();
            assertEquals(1, result.recipes().items().size());
            assertEquals(RecipeStatus.DRAFT, result.recipes().items().getFirst().status());
        }

        @Test
        void shouldForwardPaginationParamsToService() {
            var stats = UserRecipeStats.ZERO;
            var authorRecipesPage = new AuthorRecipesPage(stats, new PageResult<>(List.of(), 0L));

            when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.findRecipesByAuthor("chef_test", RecipeStatus.PUBLISHED, 2, 5)).thenReturn(authorRecipesPage);

            var pagination = new RecipePaginationDTO();
            pagination.setStatus("PUBLISHED");
            pagination.setPage(2);
            pagination.setSize(5);

            recipeController.myRecipes(pagination);

            verify(recipeService).findRecipesByAuthor("chef_test", RecipeStatus.PUBLISHED, 2, 5);
        }

        @Test
        void shouldExposeAllStatsInResponse() {
            var stats = new UserRecipeStats(3, 1, 2, 1);
            var authorRecipesPage = new AuthorRecipesPage(stats, new PageResult<>(List.of(), 0L));

            when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.findRecipesByAuthor("chef_test", RecipeStatus.PUBLISHED, 0, 10)).thenReturn(authorRecipesPage);

            var result = (AuthorRecipesPage) ((ItemResponse<?>) Objects.requireNonNull(recipeController.myRecipes(defaultPagination()).getBody())).resource();

            assertEquals(3, result.stats().published());
            assertEquals(1, result.stats().draft());
            assertEquals(2, result.stats().pendingReview());
            assertEquals(1, result.stats().rejected());
        }

        @Test
        void shouldIncludeRejectionInfoForRejectedRecipe() {
            var rejectionInfo = new RecipeRejectionInfo("Contenu inapproprié", java.time.Instant.now());
            var summary = new RecipeSummary(UUID.randomUUID(), "Recette refusée", "desc", null, 20, null, "chef_test");
            var authorSummary = new AuthorRecipeSummary(summary, RecipeStatus.REJECTED, rejectionInfo);
            var stats = new UserRecipeStats(0, 0, 0, 1);
            var page = new PageResult<>(List.of(authorSummary), 1L);
            var authorRecipesPage = new AuthorRecipesPage(stats, page);

            when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.findRecipesByAuthor("chef_test", RecipeStatus.REJECTED, 0, 10)).thenReturn(authorRecipesPage);

            var pagination = new RecipePaginationDTO();
            pagination.setStatus("REJECTED");
            pagination.setPage(0);
            pagination.setSize(10);

            var result = (AuthorRecipesPage) ((ItemResponse<?>) Objects.requireNonNull(recipeController.myRecipes(pagination).getBody())).resource();

            assertNotNull(result.recipes().items().getFirst().rejectionInfo());
            assertEquals("Contenu inapproprié", result.recipes().items().getFirst().rejectionInfo().justification());
        }
    }

    // ========== CreateVariant ==========

    @Nested
    class CreateVariant {

        @Test
        void shouldCreateVariantWithProvidedImageUrl() {
            var parentId = UUID.randomUUID();
            var savedVariant = createRecipe(UUID.randomUUID(), "Variante", parentId, RecipeStatus.PENDING_REVIEW);
            var request = new CreateRecipeRequest(parentId, "Variante", "Résumé", 30, false, "https://img.example.com/parent.jpg", List.of(), List.of(), List.of(), List.of());

            when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.createRecipe(any(CreateRecipeCommand.class))).thenReturn(savedVariant);

            var response = recipeController.createRecipe(request, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(CreatedResponse.class, response.getBody());
            // Le contrôleur ne consulte jamais le parent — c'est la responsabilité du client
            verify(recipeService, never()).findById(any());
            verify(recipeService).createRecipe(any(CreateRecipeCommand.class));
        }

        @Test
        void shouldCreateVariantWithNewImageWhenImageProvided() {
            var parentId = UUID.randomUUID();
            var savedVariant = createRecipe(UUID.randomUUID(), "Variante", parentId, RecipeStatus.PENDING_REVIEW);
            var request = new CreateRecipeRequest(parentId, "Variante", "Résumé", 30, false, null, List.of(), List.of(), List.of(), List.of());
            var mockImage = mock(MultipartFile.class);

            when(mockImage.isEmpty()).thenReturn(false);
            when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.createRecipe(any(CreateRecipeCommand.class))).thenReturn(savedVariant);

            var response = recipeController.createRecipe(request, mockImage);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(CreatedResponse.class, response.getBody());
            verify(recipeService, never()).findById(any());
            verify(recipeService).createRecipe(any(CreateRecipeCommand.class));
        }

        @Test
        void shouldThrowWhenRequestIsNull() {
            assertThrows(NullPointerException.class, () -> recipeController.createRecipe(null, null));
        }
    }

    // ========== LikeUnlikeRecipe ==========

    @Nested
    class LikeUnlikeRecipe {

        @Test
        void ShouldReturnOkWhenLikeGoesWell() {
            var user = createUser(UUID.randomUUID());
            when(userService.getUserByUsername(any())).thenReturn(user);
            doNothing().when(recipeService).likeRecipe(any(), any());
            when(authPort.extractUsername()).thenReturn(user.username());

            var response = recipeController.likeRecipe(UUID.randomUUID());
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        void ShouldReturnOkWhenUnLikeGoesWell() {
            var user = createUser(UUID.randomUUID());
            when(userService.getUserByUsername(any())).thenReturn(user);
            doNothing().when(recipeService).unlikeRecipe(any(), any());
            when(authPort.extractUsername()).thenReturn(user.username());

            var response = recipeController.unlikeRecipe(UUID.randomUUID());
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        void ShouldReturnErrorWhenUserUnFoundWhenLike() {
            var user = createUser(UUID.randomUUID());
            when(userService.getUserByUsername(any())).thenThrow(new ResourceNotFoundException("User not found with username: PaxGPT"));
            when(authPort.extractUsername()).thenReturn(user.username());

            assertThrows(ResourceNotFoundException.class, () -> recipeController.likeRecipe(UUID.randomUUID()));
        }

        @Test
        void ShouldReturnErrorWhenUserUnFoundWhenUnlike() {
            var user = createUser(UUID.randomUUID());
            when(userService.getUserByUsername(any())).thenThrow(new ResourceNotFoundException("User not found with username: PaxGPT"));
            when(authPort.extractUsername()).thenReturn(user.username());

            assertThrows(ResourceNotFoundException.class, () -> recipeController.unlikeRecipe(UUID.randomUUID()));
        }

        @Test
        void ShouldReturnErrorWhenRecipeNotFoundWhenLike() {
            var user = createUser(UUID.randomUUID());
            when(userService.getUserByUsername(any())).thenReturn(user);
            doThrow(new ResourceNotFoundException("Recipe not found with username: PaxGPT")).when(recipeService).likeRecipe(any(), any());
            when(authPort.extractUsername()).thenReturn(user.username());

            assertThrows(ResourceNotFoundException.class, () -> recipeController.likeRecipe(UUID.randomUUID()));
        }

        @Test
        void ShouldReturnErrorWhenRecipeNotFoundWhenUnlike() {
            var user = createUser(UUID.randomUUID());
            when(userService.getUserByUsername(any())).thenReturn(user);
            doThrow(new ResourceNotFoundException("Recipe not found with username: PaxGPT")).when(recipeService).unlikeRecipe(any(), any());
            when(authPort.extractUsername()).thenReturn(user.username());

            assertThrows(ResourceNotFoundException.class, () -> recipeController.unlikeRecipe(UUID.randomUUID()));
        }
    }

    // ========== ReportRecipe ==========

    @Nested
    class ReportRecipe {

        @Test
        void shouldReturn201_WhenReportCreatedSuccessfully() throws Exception {
            var recipeId = UUID.randomUUID();
            var reporterId = UUID.randomUUID();
            var request = new RecipeReportRequestDTO(RecipeReportType.SPAM, "Ceci est un spam");
            var report = new RecipeReport(
                    UUID.randomUUID(), recipeId, reporterId,
                    RecipeReportType.SPAM, ReportStatus.PENDING,
                    "Ceci est un spam", Instant.now(), null, null
            );

            when(authPort.extractUsername()).thenReturn("reporter");
            when(recipeReportService.reportRecipe(any())).thenReturn(report);

            mockMvc.perform(post("/api/recipes/{id}/reports", recipeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.resource.recipeId").value(recipeId.toString()))
                    .andExpect(jsonPath("$.resource.status").value("PENDING"))
                    .andExpect(jsonPath("$.resource.reportType").value("SPAM"));

            verify(recipeReportService).reportRecipe(any());
        }

        @Test
        void shouldReturn404_WhenRecipeDoesNotExist() throws Exception {
            var recipeId = UUID.randomUUID();
            var request = new RecipeReportRequestDTO(RecipeReportType.SPAM, "Justification");

            when(authPort.extractUsername()).thenReturn("reporter");
            when(recipeReportService.reportRecipe(any())).thenThrow(new RecipeNotFoundException(recipeId));

            mockMvc.perform(post("/api/recipes/{id}/reports", recipeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value("NOT_FOUND"));
        }

        @Test
        void shouldReturn409_WhenAlreadyReported() throws Exception {
            var recipeId = UUID.randomUUID();
            var reporterId = UUID.randomUUID();
            var request = new RecipeReportRequestDTO(RecipeReportType.SPAM, "Justification");

            when(authPort.extractUsername()).thenReturn("reporter");
            when(recipeReportService.reportRecipe(any()))
                    .thenThrow(new RecipeAlreadyReportedException(recipeId, reporterId));

            mockMvc.perform(post("/api/recipes/{id}/reports", recipeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value("CONFLICT"));
        }
    }

    @Nested
    class GetRecipesFeedTest {

        @Nested
        class WhenInstantIsAbsent {

            @Test
            void shouldCallServiceWithCurrentInstant() throws Exception {
                when(recipeService.getPersonalizedFeedRecipes(any(Instant.class), eq(0)))
                        .thenReturn(new PageResult<>(List.of(), 0L));

                mockMvc.perform(get("/api/recipes/following"))
                        .andExpect(status().isOk());

                verify(recipeService).getPersonalizedFeedRecipes(any(Instant.class), eq(0));
            }
        }

        @Nested
        class WhenInstantIsProvided {

            @Test
            void shouldCallServiceWithProvidedInstant() throws Exception {
                var fixedInstant = Instant.parse("2024-01-15T10:00:00Z");
                when(recipeService.getPersonalizedFeedRecipes(eq(fixedInstant), eq(0)))
                        .thenReturn(new PageResult<>(List.of(), 0L));

                mockMvc.perform(get("/api/recipes/following")
                                .param("instant", "2024-01-15T10:00:00Z"))
                        .andExpect(status().isOk());

                verify(recipeService).getPersonalizedFeedRecipes(eq(fixedInstant), eq(0));
            }
        }

        @Nested
        class WhenPageIsProvided {

            @Test
            void shouldCallServiceWithProvidedPage() throws Exception {
                when(recipeService.getPersonalizedFeedRecipes(any(Instant.class), eq(2)))
                        .thenReturn(new PageResult<>(List.of(), 0L));

                mockMvc.perform(get("/api/recipes/following")
                                .param("page", "2"))
                        .andExpect(status().isOk());

                verify(recipeService).getPersonalizedFeedRecipes(any(Instant.class), eq(2));
            }

            @Test
            void shouldDefaultToPageZeroWhenAbsent() throws Exception {
                when(recipeService.getPersonalizedFeedRecipes(any(Instant.class), eq(0)))
                        .thenReturn(new PageResult<>(List.of(), 0L));

                mockMvc.perform(get("/api/recipes/following"))
                        .andExpect(status().isOk());

                verify(recipeService).getPersonalizedFeedRecipes(any(Instant.class), eq(0));
            }
        }

        @Nested
        class Response {

            @Test
            void shouldReturn200() throws Exception {
                when(recipeService.getPersonalizedFeedRecipes(any(Instant.class), anyInt()))
                        .thenReturn(new PageResult<>(List.of(), 0L));

                mockMvc.perform(get("/api/recipes/following"))
                        .andExpect(status().isOk());
            }

            @Test
            void shouldReturnMappedDTOs() throws Exception {
                var summary = aPersonalizedSummary();
                when(recipeService.getPersonalizedFeedRecipes(any(Instant.class), anyInt()))
                        .thenReturn(new PageResult<>(List.of(summary), 1L));

                var result = mockMvc.perform(get("/api/recipes/following"))
                        .andExpect(status().isOk())
                        .andReturn();

                var body = result.getResponse().getContentAsString();
                assertTrue(body.contains(summary.summary().id().toString()));
            }

            @Test
            void shouldReturnTotalInResponse() throws Exception {
                when(recipeService.getPersonalizedFeedRecipes(any(Instant.class), anyInt()))
                        .thenReturn(new PageResult<>(List.of(), 42L));

                var result = mockMvc.perform(get("/api/recipes/following"))
                        .andExpect(status().isOk())
                        .andReturn();

                var body = result.getResponse().getContentAsString();
                assertTrue(body.contains("42"));
            }
        }

        // --- helpers ---

        private PersonalizedRecipeSummary aPersonalizedSummary() {
            var recipe = new RecipeSummary(UUID.randomUUID(), "Pasta", "", "", 0, Instant.now(), ""/* autres champs */);
            return new PersonalizedRecipeSummary(recipe, RecipeCounts.ZERO, RecipeUserInteraction.NONE);
        }
    }
    // ========== Helpers ==========

    private User createUser(UUID id) {
        return new User(id, "PaxGPT", "Pax", "Pekpa", "a@gmail.com",
                UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL, null, null, true);
    }

    private PersonalizedRecipeSummary createPersonalizedRecipeSummary(UUID id, String title) {
        var summary = new RecipeSummary(id, title, "Summary for " + title, null, 30, now, "chef_test");
        return new PersonalizedRecipeSummary(summary, RecipeCounts.ZERO, RecipeUserInteraction.NONE);
    }

    private Recipe createRecipe(UUID id, String title, UUID parentId, RecipeStatus status) {
        return new Recipe(
                id, title, "Summary for " + title, parentId,
                "chef_test", 30, null, status,
                List.of(), List.of(), List.of(), List.of(), now, now
        );
    }

    private PersonalizedRecipe createRecipeWithMetadata(UUID id, String title, UUID parentId, RecipeStatus status) {
        return new PersonalizedRecipe(
                createRecipe(id, title, parentId, status),
                RecipeCounts.ZERO,
                RecipeUserInteraction.NONE
        );
    }
}
