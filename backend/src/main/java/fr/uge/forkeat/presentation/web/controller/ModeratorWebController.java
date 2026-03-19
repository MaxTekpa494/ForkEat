package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.service.RecipeModerationActionService;
import fr.uge.forkeat.service.RecipeService;
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

    private final RecipeService recipeService;
    private final RecipeModerationActionService recipeModerationActionService;
    private final AuthenticationPort authPort;

    public ModeratorWebController(RecipeService recipeService,
                                  RecipeModerationActionService recipeModerationActionService,
                                  AuthenticationPort authPort) {
        this.recipeService = recipeService;
        this.recipeModerationActionService = recipeModerationActionService;
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
}