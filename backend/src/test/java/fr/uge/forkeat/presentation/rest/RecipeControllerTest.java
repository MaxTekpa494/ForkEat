package fr.uge.forkeat.presentation.rest;

import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.presentation.rest.controller.RecipeRestController;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import org.junit.jupiter.api.BeforeEach;
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

    private RecipeRestController recipeController;
    private Instant now;

    @BeforeEach
    void setUp() {
        recipeController = new RecipeRestController(recipeService);
        now = Instant.now();
    }

    @Test
    void constructor_shouldThrowWhenServiceIsNull() {
        assertThrows(NullPointerException.class, () -> new RecipeRestController(null));
    }

    // ========== getRecipe tests ==========

    @Test
    void getRecipe_shouldReturnRecipeWhenFound() {
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
    void getRecipe_shouldReturnRecipeWithParentWhenVariant() {
        var parentId = UUID.randomUUID();
        var childId = UUID.randomUUID();
        var parentRecipe = createRecipe(parentId, "Recette originale", null, RecipeStatus.PUBLISHED);
        var childRecipe = createRecipe(childId, "Variante", parentId, RecipeStatus.DRAFT);

        when(recipeService.findById(childId)).thenReturn(childRecipe);
        when(recipeService.findById(parentId)).thenReturn(parentRecipe);

        var response = recipeController.getRecipe(childId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        var itemResponse = (ItemResponse<?>) response.getBody();
        assertNotNull(itemResponse);
        verify(recipeService).findById(childId);
        verify(recipeService).findById(parentId);
    }

    @Test
    void getRecipe_shouldNotFetchParentWhenNotVariant() {
        var recipeId = UUID.randomUUID();
        var recipe = createRecipe(recipeId, "Recette simple", null, RecipeStatus.PUBLISHED);
        when(recipeService.findById(recipeId)).thenReturn(recipe);

        recipeController.getRecipe(recipeId);

        verify(recipeService, times(1)).findById(recipeId);
        verifyNoMoreInteractions(recipeService);
    }

    @Test
    void getRecipe_shouldPropagateExceptionWhenNotFound() {
        var recipeId = UUID.randomUUID();
        when(recipeService.findById(recipeId)).thenThrow(new RecipeNotFoundException(recipeId));

        assertThrows(RecipeNotFoundException.class, () -> recipeController.getRecipe(recipeId));
        verify(recipeService).findById(recipeId);
    }

    // ========== getRecipes tests ==========

    @Test
    void getRecipes_shouldReturnPaginatedList() {
        var recipe1 = createRecipe(UUID.randomUUID(), "Recette 1", null, RecipeStatus.PUBLISHED);
        var recipe2 = createRecipe(UUID.randomUUID(), "Recette 2", null, RecipeStatus.PUBLISHED);
        var pageResult = new PageResult<>(List.of(recipe1, recipe2), 10);

        when(recipeService.findByStatus("PUBLISHED", 10, 0)).thenReturn(pageResult);

        var response = recipeController.getRecipes("PUBLISHED", 10, 0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(ListResponse.class, response.getBody());
        var listResponse = (ListResponse<?>) response.getBody();
        assertEquals(2, listResponse.resources().size());
        assertEquals(10, listResponse.total());
        verify(recipeService).findByStatus("PUBLISHED", 10, 0);
    }

    @Test
    void getRecipes_shouldReturnEmptyListWhenNoRecipes() {
        var pageResult = new PageResult<Recipe>(List.of(), 0);
        when(recipeService.findByStatus("DRAFT", 10, 0)).thenReturn(pageResult);

        var response = recipeController.getRecipes("DRAFT", 10, 0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        var listResponse = (ListResponse<?>) response.getBody();
        assertTrue(listResponse.resources().isEmpty());
        assertEquals(0, listResponse.total());
    }

    @Test
    void getRecipes_shouldRespectSizeParameter() {
        var recipe = createRecipe(UUID.randomUUID(), "Recette", null, RecipeStatus.PUBLISHED);
        var pageResult = new PageResult<>(List.of(recipe), 100);

        when(recipeService.findByStatus("PUBLISHED", 5, 0)).thenReturn(pageResult);

        recipeController.getRecipes("PUBLISHED", 5, 0);

        verify(recipeService).findByStatus("PUBLISHED", 5, 0);
    }

    @Test
    void getRecipes_shouldRespectPageParameter() {
        var recipe = createRecipe(UUID.randomUUID(), "Recette", null, RecipeStatus.PUBLISHED);
        var pageResult = new PageResult<>(List.of(recipe), 100);

        when(recipeService.findByStatus("PUBLISHED", 10, 2)).thenReturn(pageResult);

        recipeController.getRecipes("PUBLISHED", 10, 2);

        verify(recipeService).findByStatus("PUBLISHED", 10, 2);
    }

    @Test
    void getRecipes_shouldFilterByStatus() {
        var draftRecipe = createRecipe(UUID.randomUUID(), "Brouillon", null, RecipeStatus.DRAFT);
        var pageResult = new PageResult<>(List.of(draftRecipe), 1);

        when(recipeService.findByStatus("DRAFT", 10, 0)).thenReturn(pageResult);

        var response = recipeController.getRecipes("DRAFT", 10, 0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(recipeService).findByStatus("DRAFT", 10, 0);
    }

    @Test
    void getRecipes_shouldHandlePendingReviewStatus() {
        var pendingRecipe = createRecipe(UUID.randomUUID(), "En attente", null, RecipeStatus.PENDING_REVIEW);
        var pageResult = new PageResult<>(List.of(pendingRecipe), 1);

        when(recipeService.findByStatus("PENDING_REVIEW", 10, 0)).thenReturn(pageResult);

        var response = recipeController.getRecipes("PENDING_REVIEW", 10, 0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(recipeService).findByStatus("PENDING_REVIEW", 10, 0);
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
