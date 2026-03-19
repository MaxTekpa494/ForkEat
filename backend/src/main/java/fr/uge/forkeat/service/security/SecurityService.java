package fr.uge.forkeat.service.security;


import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.model.recipe.Recipe;
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

    public SecurityService(AuthenticationPort authPort) {
        this.authPort = authPort;
    }

    private boolean canAlterRecipe(Recipe recipeToAlter){
        var username = authPort.extractUsername();
        return recipeToAlter.usernameAuthor().equals(username);
    }

    public boolean canUpdateRecipe(Recipe recipeToUpdate){
        return canAlterRecipe(recipeToUpdate);
    }

    public boolean canDeleteRecipe(Recipe recipeToDelete){
        return canAlterRecipe(recipeToDelete);
    }

}
