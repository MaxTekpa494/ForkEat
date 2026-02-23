package fr.uge.forkeat.presentation.rest;

import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.dto.recipe.RecipeSearchDTO;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.presentation.rest.controller.RecipeRestController;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeSearchCriteria;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeControllerTest {

    @Mock
    private RecipeService recipeService;

    @Mock
    private AuthenticationPort authPort;

    private RecipeRestController recipeController;
    private Instant now;

    @BeforeEach
    void setUp() {
        recipeController = new RecipeRestController(recipeService, authPort);
        now = Instant.now();
    }

    @Nested
    class CreateRecipe {

        @Test
        void shouldCreateRecipeWithAuthenticatedUsername() {
            var recipeId = UUID.randomUUID();
            var savedRecipe = createRecipe(recipeId, "Tarte aux pommes", null, RecipeStatus.DRAFT);
            var dto = new RecipeDTO(null, "Tarte aux pommes", "Une bonne tarte", null,
                    null, 30, null, "DRAFT", List.of(), List.of(), List.of(), Map.of(), null, null);

            when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipeService.createRecipe(any(), any())).thenReturn(savedRecipe);

            var response = recipeController.createRecipe(dto);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(ItemResponse.class, response.getBody());
            verify(authPort).extractUsername();
            verify(recipeService).createRecipe(any(), any());
        }

        @Test
        void shouldUseAuthenticatedUsernameNotDtoUsername() {
            var recipeId = UUID.randomUUID();
            var savedRecipe = createRecipe(recipeId, "Recette", null, RecipeStatus.DRAFT);
            var dto = new RecipeDTO(null, "Recette", "Résumé", null,
                    "intruder", 20, null, "DRAFT", List.of(), List.of(), List.of(), Map.of(), null, null);

            when(authPort.extractUsername()).thenReturn("real_author");
            when(recipeService.createRecipe(any(), any())).thenReturn(savedRecipe);

            recipeController.createRecipe(dto);

            verify(authPort).extractUsername();
        }
    }

    @Nested
    class GetRecipe {

        @Test
        void shouldReturnRecipeWhenFound() {
            var recipeId = UUID.randomUUID();
            var recipe = createRecipe(recipeId, "Tarte aux pommes", null, RecipeStatus.PUBLISHED);
            when(recipeService.findById(recipeId)).thenReturn(recipe);

            var response = recipeController.getRecipe(recipeId);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(ItemResponse.class, response.getBody());
            var itemResponse = (ItemResponse<?>) response.getBody();
            assertNotNull(itemResponse.resource());
            verify(recipeService).findById(recipeId);
        }

        @Test
        void shouldReturnRecipeWithParentIdWhenVariant() {
            var parentId = UUID.randomUUID();
            var childId = UUID.randomUUID();
            var childRecipe = createRecipe(childId, "Variante", parentId, RecipeStatus.DRAFT);

            when(recipeService.findById(childId)).thenReturn(childRecipe);

            var response = recipeController.getRecipe(childId);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            var itemResponse = (ItemResponse<?>) response.getBody();
            assertNotNull(itemResponse);
            verify(recipeService, times(1)).findById(childId);
            verifyNoMoreInteractions(recipeService);
        }

        @Test
        void shouldNotFetchParentWhenNotVariant() {
            var recipeId = UUID.randomUUID();
            var recipe = createRecipe(recipeId, "Recette simple", null, RecipeStatus.PUBLISHED);
            when(recipeService.findById(recipeId)).thenReturn(recipe);

            recipeController.getRecipe(recipeId);

            verify(recipeService, times(1)).findById(recipeId);
            verifyNoMoreInteractions(recipeService);
        }

        @Test
        void shouldPropagateExceptionWhenNotFound() {
            var recipeId = UUID.randomUUID();
            when(recipeService.findById(recipeId)).thenThrow(new RecipeNotFoundException(recipeId));

            assertThrows(RecipeNotFoundException.class, () -> recipeController.getRecipe(recipeId));
            verify(recipeService).findById(recipeId);
        }
    }

    @Nested
    class GetRecipes {

        @Test
        void shouldReturnPaginatedList() {
            var recipe1 = createRecipe(UUID.randomUUID(), "Recette 1", null, RecipeStatus.PUBLISHED);
            var recipe2 = createRecipe(UUID.randomUUID(), "Recette 2", null, RecipeStatus.PUBLISHED);
            var pageResult = new PageResult<>(List.of(recipe1, recipe2), 10);
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
            var pageResult = new PageResult<Recipe>(List.of(), 0);
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
            var recipe = createRecipe(UUID.randomUUID(), "Recette", null, RecipeStatus.PUBLISHED);
            var pageResult = new PageResult<>(List.of(recipe), 100);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, null, List.of(), 5, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            recipeController.getRecipes(new RecipeSearchDTO("PUBLISHED", 5, 0, null, null));

            verify(recipeService).searchRecipes(criteria);
        }

        @Test
        void shouldRespectPageParameter() {
            var recipe = createRecipe(UUID.randomUUID(), "Recette", null, RecipeStatus.PUBLISHED);
            var pageResult = new PageResult<>(List.of(recipe), 100);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, null, List.of(), 12, 2);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            recipeController.getRecipes(new RecipeSearchDTO("PUBLISHED", 12, 2, null, null));

            verify(recipeService).searchRecipes(criteria);
        }

        @Test
        void shouldFilterByStatus() {
            var draftRecipe = createRecipe(UUID.randomUUID(), "Brouillon", null, RecipeStatus.DRAFT);
            var pageResult = new PageResult<>(List.of(draftRecipe), 1);
            var criteria = new RecipeSearchCriteria(RecipeStatus.DRAFT, null, List.of(), 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            var response = recipeController.getRecipes(new RecipeSearchDTO("DRAFT", 12, 0, null, null));

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        void shouldHandlePendingReviewStatus() {
            var pendingRecipe = createRecipe(UUID.randomUUID(), "En attente", null, RecipeStatus.PENDING_REVIEW);
            var pageResult = new PageResult<>(List.of(pendingRecipe), 1);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PENDING_REVIEW, null, List.of(), 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            var response = recipeController.getRecipes(new RecipeSearchDTO("PENDING_REVIEW", 12, 0, null, null));

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Nested
    class GetRecipesWithSearch {

        @Test
        void shouldSearchWhenSearchParameterProvided() {
            var recipe = createRecipe(UUID.randomUUID(), "Tarte aux pommes", null, RecipeStatus.PUBLISHED);
            var pageResult = new PageResult<>(List.of(recipe), 1);
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

    @Nested
    class GetRecipesWithAllergens {

        @Test
        void shouldFilterByAllergensWhenProvided() {
            var recipe = createRecipe(UUID.randomUUID(), "Recette sans gluten", null, RecipeStatus.PUBLISHED);
            var allergens = List.of("gluten", "lactose");
            var pageResult = new PageResult<>(List.of(recipe), 1);
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
            var recipe = createRecipe(UUID.randomUUID(), "Tarte sans gluten", null, RecipeStatus.PUBLISHED);
            var allergens = List.of("gluten");
            var pageResult = new PageResult<>(List.of(recipe), 1);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, "tarte", allergens, 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            var response = recipeController.getRecipes(new RecipeSearchDTO("PUBLISHED", 12, 0, "tarte", allergens));

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(recipeService).searchRecipes(criteria);
            verifyNoMoreInteractions(recipeService);
        }

        @Test
        void shouldWorkWithEmptyAllergensList() {
            var recipe = createRecipe(UUID.randomUUID(), "Recette", null, RecipeStatus.PUBLISHED);
            var pageResult = new PageResult<>(List.of(recipe), 1);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, null, List.of(), 12, 0);

            when(recipeService.searchRecipes(criteria)).thenReturn(pageResult);

            var response = recipeController.getRecipes(new RecipeSearchDTO("PUBLISHED", 12, 0, null, List.of()));

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(recipeService).searchRecipes(criteria);
        }
    }

    private Recipe createRecipe(UUID id, String title, UUID parentId, RecipeStatus status) {
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
                Map.of(),
                now,
                now
        );
    }
}