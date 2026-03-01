package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ModeratorRestControllerTest {

  @Mock
  private RecipeService recipeService;

  private ModeratorRestController moderatorController;
  private Instant now;

  @BeforeEach
  void setUp() {
    moderatorController = new ModeratorRestController(recipeService);
    now = Instant.now();
  }

  @Nested
  class GetPendingRecipes {

    @Test
    void shouldFilterByPendingStatus() {
      var pendingRecipe = createRecipe(UUID.randomUUID(), "En attente", RecipeStatus.PENDING_REVIEW);
      var draftRecipe = createRecipe(UUID.randomUUID(), "Brouillon", RecipeStatus.DRAFT);
      var pageResult = new PageResult<>(List.of(pendingRecipe), 1);

      when(recipeService.findByStatus(RecipeStatus.PENDING_REVIEW, 12, 0)).thenReturn(pageResult);

      var response = moderatorController.getPendingRecipes(12, 0);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertInstanceOf(ListResponse.class, response.getBody());
      var listResponse = (ListResponse<?>) response.getBody();
      assertEquals(1, listResponse.resources().size());
      assertEquals(1, listResponse.total());
    }
  }

  @Nested
  class ValidateRecipe {

    @Test
    void ShouldReturnOkWhenRecipeisFound() {
      var recipeId = UUID.randomUUID();
      var recipe = createRecipe(recipeId, "test", RecipeStatus.PUBLISHED);
      when(recipeService.updateStatus(recipeId, RecipeStatus.PUBLISHED)).thenReturn(recipe);

      var response = moderatorController.validateRecipe(recipeId);
      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void ShouldReturnErrorWhenRecipeNotFound() {
      var recipeId = UUID.randomUUID();
      when(recipeService.updateStatus(recipeId, RecipeStatus.PUBLISHED)).thenThrow(new ResourceNotFoundException("Recipe not found"));

      assertThrows(ResourceNotFoundException.class, ()-> moderatorController.validateRecipe(recipeId));
    }

  }

  @Nested
  class RejectRecipe {

    @Test
    void ShouldReturnOkWhenRecipeisFound() {
      var recipeId = UUID.randomUUID();
      var recipe = createRecipe(recipeId, "test", RecipeStatus.REJECTED);
      when(recipeService.updateStatus(recipeId, RecipeStatus.REJECTED)).thenReturn(recipe);

      var response = moderatorController.rejectRecipe(recipeId);
      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void ShouldReturnErrorWhenRecipeNotFound() {
      var recipeId = UUID.randomUUID();
      when(recipeService.updateStatus(recipeId, RecipeStatus.REJECTED)).thenThrow(new ResourceNotFoundException("Recipe not found"));

      assertThrows(ResourceNotFoundException.class, ()-> moderatorController.rejectRecipe(recipeId));
    }

  }

  private Recipe createRecipe(UUID id, String title, RecipeStatus status) {
    return new Recipe(
            id,
            title,
            "Summary for " + title,
            null,
            "chef_test",
            30,
            null,
            status,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            now,
            now
    );
  }
}
