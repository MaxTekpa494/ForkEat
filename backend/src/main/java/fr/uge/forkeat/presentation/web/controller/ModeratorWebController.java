package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.service.PlatformWalletService;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.exception.RegisterFailureException;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.user.UserRegister;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserRegistrationService;
import fr.uge.forkeat.service.user.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
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
    private final AuthenticationPort authPort;

    public ModeratorWebController(RecipeService recipeService,
                                  AuthenticationPort authPort) {
        this.recipeService = Objects.requireNonNull(recipeService);
        this.authPort = Objects.requireNonNull(authPort);
    }

    @GetMapping("/recipes")
    public String pendingRecipes(@RequestParam(defaultValue = "0") int page, Model model) {
        var result = recipeService.findByStatus(RecipeStatus.PENDING_REVIEW, RECIPES_PAGE_SIZE, page);
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
        recipeService.updateStatus(id, RecipeStatus.PUBLISHED);
        return "redirect:/moderator/recipes";
    }

    @PostMapping("/recipes/{id}/reject")
    public String rejectRecipe(@PathVariable UUID id) {
        recipeService.updateStatus(id, RecipeStatus.REJECTED);
        return "redirect:/moderator/recipes";
    }
}