package fr.uge.forkeat.presentation.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeWebController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("pageTitle", "Accueil - ForkEat");
        
        // Statistiques (à remplacer par de vraies données @Max)
        model.addAttribute("totalRecipes", 1250);
        model.addAttribute("totalUsers", 8500);
        model.addAttribute("totalChefs", 450);
        return "home/index";
    }
}