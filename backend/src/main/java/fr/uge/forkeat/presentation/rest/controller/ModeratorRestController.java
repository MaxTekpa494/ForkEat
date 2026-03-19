package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.dto.recipe.RejectRecipeRequest;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.service.RecipeModerationActionService;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.model.recipe.CreateRecipeModerationAction;
import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/moderator")
public class ModeratorRestController {

  private final AuthenticationPort authPort;
  private final RecipeService recipeService;
  private final RecipeModerationActionService recipeModerationActionService;

  private final Logger logger = LoggerFactory.getLogger(ModeratorRestController.class);

  public ModeratorRestController(AuthenticationPort authPort,
                                 RecipeService recipeService,
                                 RecipeModerationActionService recipeModerationActionService) {
    this.authPort = authPort;
    this.recipeService = recipeService;
    this.recipeModerationActionService = recipeModerationActionService;
  }

  @GetMapping("/recipes/pending")
  public ResponseEntity<HttpResponse<RecipeDTO>> getPendingRecipes(int size, int page) {
    var pageResult = recipeService.getRecipesToModerate(authPort.extractUsername(), size, page);
    var dtos = pageResult.items().stream().map(RecipeDTOMapper::toDTO).toList();
    return ResponseEntity.ok(new ListResponse<>(dtos, pageResult.total()));
  }

  @PostMapping("/recipes/{id}/validate")
  public ResponseEntity<Void> validateRecipe(@PathVariable UUID id) {
    recipeModerationActionService.moderateRecipe(new CreateRecipeModerationAction(id, authPort.extractUsername(), RecipeModerationActionType.APPROVED, "", null));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/recipes/{id}/reject")
  public ResponseEntity<Void> rejectRecipe(@PathVariable UUID id, @RequestBody RejectRecipeRequest request) {
    recipeModerationActionService.moderateRecipe(new CreateRecipeModerationAction(id, authPort.extractUsername(), RecipeModerationActionType.REJECTED, request.justification(), null));
    return ResponseEntity.noContent().build();
  }

}
