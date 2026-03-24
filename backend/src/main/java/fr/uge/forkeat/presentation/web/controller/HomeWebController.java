package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeWebController {

    private final RecipeService recipeService;
    private final AuthenticationPort authPort;

    HomeWebController(RecipeService recipeService, AuthenticationPort authPort) {
        this.recipeService = recipeService;
        this.authPort = authPort;
    }

    @GetMapping("/error/403")
    public String forbidden() {
        return "error/403";
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("pageTitle", "Accueil - ForkEat");

        // Statistiques (à remplacer par de vraies données @Max)
        model.addAttribute("totalRecipes", 1250);
        model.addAttribute("totalUsers", 8500);
        model.addAttribute("totalChefs", 450);

        var currentUsername = authPort.extractUsername();
        recipeService.getTopLikedRecipe(currentUsername)
                .map(RecipeDTOMapper::toSummaryDTO)
                .ifPresent(r -> model.addAttribute("topRecipe", r));

        return "home/index";
    }
}