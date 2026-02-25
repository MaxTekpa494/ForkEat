package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/moderator")
public class ModeratorRestController {

  private final RecipeService recipeService;

  private final Logger logger = LoggerFactory.getLogger(ModeratorRestController.class);

  public ModeratorRestController(RecipeService recipeService) {
    this.recipeService = recipeService;
  }

  @GetMapping("/recipes/pending")
  public ResponseEntity<HttpResponse<RecipeDTO>> getPendingRecipes(int size, int page) {
    var pageResult = recipeService.findByStatus(RecipeStatus.PENDING_REVIEW, size, page);

    logger.debug("Liste recettes pending : {}", pageResult);
    var dtos = pageResult.items().stream().map(RecipeDTOMapper::toDTO).toList();
    return ResponseEntity.ok(new ListResponse<>(dtos, pageResult.total()));
  }

  @PostMapping("/recipes/{id}/validate")
  public ResponseEntity<?> validateRecipe(@PathVariable UUID id) {
    Objects.requireNonNull(id);
    recipeService.updateRecipeByStatus(id, RecipeStatus.PUBLISHED);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/recipes/{id}/reject")
  public ResponseEntity<?> rejectRecipe(@PathVariable UUID id) {
    Objects.requireNonNull(id);
    recipeService.updateRecipeByStatus(id, RecipeStatus.REJECTED);
    return ResponseEntity.ok().build();
  }

}
