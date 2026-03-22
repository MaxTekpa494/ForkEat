package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.service.*;
import fr.uge.forkeat.service.model.recipe.CreateRecipeModerationAction;
import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;
import fr.uge.forkeat.service.model.user.CreateUserModerationAction;
import fr.uge.forkeat.service.model.user.UserModerationActionType;
import fr.uge.forkeat.service.port.AuthenticationPort;

import fr.uge.forkeat.presentation.web.dto.UserModerationRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static fr.uge.forkeat.presentation.ComputeSuspendedUntil.computeSuspendedUntil;

@Controller
@RequestMapping("/moderator")
public class ModeratorWebController {

    private final UserReportService userReportService;
    private final UserModerationActionService userModerationActionService;

    @ModelAttribute("inModeratorInterface")
    public boolean inModeratorInterface() {
        return true;
    }

    private static final int RECIPES_PAGE_SIZE = 10;
    private static final int RECIPESREPORTS_PAGE_SIZE = 10;
    private static final int USERSREPORTS_PAGE_SIZE = 10;

    private final RecipeService recipeService;
    private final RecipeModerationActionService recipeModerationActionService;
    private final RecipeReportService recipeReportService;
    private final AuthenticationPort authPort;

    public ModeratorWebController(RecipeService recipeService,
                                  RecipeModerationActionService recipeModerationActionService,
                                  RecipeReportService recipeReportService,
                                  AuthenticationPort authPort, UserReportService userReportService, UserModerationActionService userModerationActionService) {
        this.recipeService = recipeService;
        this.recipeModerationActionService = recipeModerationActionService;
        this.recipeReportService = recipeReportService;
        this.authPort = authPort;
        this.userReportService = userReportService;
        this.userModerationActionService = userModerationActionService;
    }

    @GetMapping("/recipes")
    public String pendingRecipes(@RequestParam(defaultValue = "0") int page, Model model) {
        var result = recipeService.getRecipesToModerate(authPort.extractUsername(), RECIPES_PAGE_SIZE, page);
        var totalPages = (int) Math.ceil((double) result.total() / RECIPES_PAGE_SIZE);

        model.addAttribute("recipes", result.items());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", result.total());
        model.addAttribute("pageTitle", "Recettes en attente - Modération");
        return "moderator/recipes-pending";
    }

    @PostMapping("/recipes/{id}/validate")
    public String validateRecipe(@PathVariable UUID id) {
        recipeModerationActionService.moderateRecipe(new CreateRecipeModerationAction(id, authPort.extractUsername(), RecipeModerationActionType.APPROVED, "", null));
        return "redirect:/moderator/recipes";
    }

    @PostMapping("/recipes/{id}/reject")
    public String rejectRecipe(@PathVariable UUID id, @RequestParam("justification") String justification) {
        recipeModerationActionService.moderateRecipe(new CreateRecipeModerationAction(id, authPort.extractUsername(), RecipeModerationActionType.REJECTED, justification, null));
        return "redirect:/moderator/recipes";
    }

    @GetMapping("/recipes/reports")
    public String reportedRecipes(@RequestParam(defaultValue = "0") int page, Model model) {
        var result = recipeReportService.getReportsToModerate(authPort.extractUsername() ,RECIPESREPORTS_PAGE_SIZE, page);
        var totalPages = (int) Math.ceil((double) result.total() / RECIPESREPORTS_PAGE_SIZE);
        model.addAttribute("reports", result.items());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", result.total());
        model.addAttribute("pageTitle", "Signalements de recettes - Modération");
        return "moderator/recipes-reports";
    }

    @GetMapping("/users/reports")
    public String reportedUsers(@RequestParam(defaultValue = "0") int page, Model model) {
        var result = userReportService.getReportsToModerate(authPort.extractUsername(), USERSREPORTS_PAGE_SIZE, page);
        var totalPages = (int) Math.ceil((double) result.total() / USERSREPORTS_PAGE_SIZE);
        model.addAttribute("reports", result.items());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", result.total());
        model.addAttribute("pageTitle", "Signalements d'utilisateurs - Administration");
        return "/moderator/users-reports";
    }

    @PostMapping("/recipes/reports/{reportId}/validate")
    public String validateRecipeReport(@PathVariable UUID reportId, @RequestParam("recipeId") UUID recipeId) {
        recipeModerationActionService.moderateRecipe(new CreateRecipeModerationAction(recipeId, authPort.extractUsername(), RecipeModerationActionType.APPROVED, "", reportId));
        return "redirect:/moderator/recipes/reports";
    }

    @PostMapping("/recipes/reports/{reportId}/dismiss")
    public String dismissRecipeReport(@PathVariable UUID reportId, @RequestParam("recipeId") UUID recipeId, @RequestParam("justification") String justification) {
        recipeModerationActionService.moderateRecipe(new CreateRecipeModerationAction(recipeId, authPort.extractUsername(), RecipeModerationActionType.REJECTED, justification, reportId));
        return "redirect:/moderator/recipes/reports";
    }

    @PostMapping("/users/reports/{reportId}/resolve")
    public String resolveUserReport(@PathVariable UUID reportId, @ModelAttribute UserModerationRequest request) {
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
        return "redirect:/moderator/users/reports";
    }

}