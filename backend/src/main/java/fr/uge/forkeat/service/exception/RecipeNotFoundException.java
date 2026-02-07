package fr.uge.forkeat.service.exception;

import java.util.UUID;

public class RecipeNotFoundException extends RuntimeException {

    private final UUID recipeId;

    public RecipeNotFoundException(UUID recipeId) {
        super("Recipe not found with id: " + recipeId);
        this.recipeId = recipeId;
    }

    public RecipeNotFoundException(UUID recipeId, Throwable cause) {
        super("Recipe not found with id: " + recipeId, cause);
        this.recipeId = recipeId;
    }

    public UUID getRecipeId() {
        return recipeId;
    }
}