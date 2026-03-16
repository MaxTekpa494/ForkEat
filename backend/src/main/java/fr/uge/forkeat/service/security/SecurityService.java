package fr.uge.forkeat.service.security;


import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;


/*
Service to handle the security logic
 */

@Service("securityService")
public class SecurityService {

    private final AuthenticationPort authPort;
    private final RecipeService recipeService;

    public SecurityService(AuthenticationPort authPort, RecipeService recipeService) {
        this.authPort = authPort;
        this.recipeService = recipeService;
    }

    private boolean canAlterRecipe(UUID recipeId){
        var username = authPort.extractUsername();
        var recipeToAlter = recipeService.findById(recipeId);
        return recipeToAlter.usernameAuthor().equals(username);
    }

    public boolean canUpdateRecipe(UUID recipeId){
        return canAlterRecipe(recipeId);
    }

    public boolean canDeleteRecipe(UUID recipeId){
        return canAlterRecipe(recipeId);
    }

}
