package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;
import fr.uge.forkeat.presentation.dto.recipe.RejectRecipeRequest;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.presentation.web.dto.UserModerationRequest;
import fr.uge.forkeat.service.*;
import fr.uge.forkeat.service.model.recipe.CreateRecipeModerationAction;
import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;
import fr.uge.forkeat.service.model.recipe.projection.RecipeReportDetails;
import fr.uge.forkeat.service.model.user.CreateUserModerationAction;
import fr.uge.forkeat.service.model.user.projection.UserReportDetails;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import fr.uge.forkeat.presentation.dto.recipe.ValidateReportRequest;
import fr.uge.forkeat.presentation.dto.recipe.DismissReportRequest;

import static fr.uge.forkeat.presentation.ComputeSuspendedUntil.computeSuspendedUntil;

@RestController
@RequestMapping("/api/moderator")
public class ModeratorRestController {

  private final AuthenticationPort authPort;
  private final RecipeService recipeService;
  private final RecipeModerationActionService recipeModerationActionService;
  private final RecipeReportService recipeReportService;
  private final UserReportService userReportService;
  private final UserModerationActionService userModerationActionService;

  private final Logger logger = LoggerFactory.getLogger(ModeratorRestController.class);

  public ModeratorRestController(AuthenticationPort authPort,
                                 RecipeService recipeService,
                                 RecipeModerationActionService recipeModerationActionService,
                                 RecipeReportService recipeReportService,
                                 UserReportService userReportService,
                                 UserModerationActionService userModerationActionService) {
    this.authPort = authPort;
    this.recipeService = recipeService;
    this.recipeModerationActionService = recipeModerationActionService;
    this.recipeReportService = recipeReportService;
    this.userReportService = userReportService;
    this.userModerationActionService = userModerationActionService;
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

  @GetMapping("/recipes/reports")
  public ResponseEntity<HttpResponse<RecipeReportDetails>> getReportedRecipes(@RequestParam int size, @RequestParam int page) {
    var pageResult = recipeReportService.getReportsToModerate(authPort.extractUsername(), size, page);
    return ResponseEntity.ok(new ListResponse<>(pageResult.items(), pageResult.total()));
  }

  @GetMapping("/users/reports")
  public ResponseEntity<HttpResponse<UserReportDetails>> getReportedUsers(@RequestParam int size, @RequestParam int page) {
    var pageResult = userReportService.getReportsToModerate(authPort.extractUsername(), size, page);
    return ResponseEntity.ok(new ListResponse<>(pageResult.items(), pageResult.total()));
  }


  @PostMapping("/recipes/reports/{reportId}/validate")
  public ResponseEntity<Void> validateReport(@PathVariable UUID reportId, @RequestBody ValidateReportRequest request) {
    recipeModerationActionService.moderateRecipe(new CreateRecipeModerationAction(
      request.recipeId(),
      authPort.extractUsername(),
      RecipeModerationActionType.APPROVED,
      "",
      reportId
    ));
    return ResponseEntity.noContent().build();
  }


  @PostMapping("/recipes/reports/{reportId}/dismiss")
  public ResponseEntity<Void> dismissReport(@PathVariable UUID reportId, @RequestBody DismissReportRequest request) {
    recipeModerationActionService.moderateRecipe(new CreateRecipeModerationAction(
      request.recipeId(),
      authPort.extractUsername(),
      RecipeModerationActionType.REJECTED,
      request.justification(),
      reportId
    ));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/users/reports/{reportId}/resolve")
  public ResponseEntity<Void> resolveUserReport(@PathVariable UUID reportId, @RequestBody UserModerationRequest request) {
    var suspendedUntil = computeSuspendedUntil(request.action(), request.suspensionDays(), request.suspensionHours());
    userModerationActionService.moderateUser(
            new CreateUserModerationAction(
                    request.userId(),
                    authPort.extractUsername(),
                    request.action(),
                    request.justification(),
                    suspendedUntil,
                    reportId
            )
    );
    return ResponseEntity.noContent().build();
  }

}
