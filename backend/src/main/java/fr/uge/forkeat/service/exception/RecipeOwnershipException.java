package fr.uge.forkeat.service.exception;

import java.util.UUID;

public class RecipeOwnershipException extends RuntimeException {

    public RecipeOwnershipException(UUID recipeId, String username) {
        super("User '" + username + "' is not the owner of recipe " + recipeId);
    }
}
