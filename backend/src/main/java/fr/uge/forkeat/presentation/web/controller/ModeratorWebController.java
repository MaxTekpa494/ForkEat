package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.service.RecipeModerationActionService;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.RecipeReportService;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.recipe.CreateRecipeModerationAction;
import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/moderator")
public class ModeratorWebController {

    @ModelAttribute("inModeratorInterface")
    public boolean inModeratorInterface() {
        return true;
    }

    private static final int RECIPES_PAGE_SIZE = 10;
    private static final int RECIPESREPORTS_PAGE_SIZE = 10;

    private final RecipeService recipeService;
    private final RecipeModerationActionService recipeModerationActionService;
    private final RecipeReportService recipeReportService;
    private final AuthenticationPort authPort;

    public ModeratorWebController(RecipeService recipeService,
                                  RecipeModerationActionService recipeModerationActionService,
                                  RecipeReportService recipeReportService,
                                  AuthenticationPort authPort) {
        this.recipeService = recipeService;
        this.recipeModerationActionService = recipeModerationActionService;
        this.recipeReportService = recipeReportService;
        this.authPort = authPort;
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

    @PostMapping("/reports/{reportId}/validate")
    public String validateReport(@PathVariable UUID reportId, @RequestParam("recipeId") UUID recipeId) {
        recipeModerationActionService.moderateRecipe(new CreateRecipeModerationAction(recipeId, authPort.extractUsername(), RecipeModerationActionType.APPROVED, "", reportId));
        return "redirect:/moderator/recipes/reports";
    }

    @PostMapping("/reports/{reportId}/dismiss")
    public String dismissReport(@PathVariable UUID reportId, @RequestParam("recipeId") UUID recipeId, @RequestParam("justification") String justification) {
        recipeModerationActionService.moderateRecipe(new CreateRecipeModerationAction(recipeId, authPort.extractUsername(), RecipeModerationActionType.REJECTED, justification, reportId));
        return "redirect:/moderator/recipes/reports";
    }
}