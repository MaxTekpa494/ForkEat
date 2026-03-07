package fr.uge.forkeat.service.exception;

import java.util.UUID;

public class RecipeAlreadyReportedException extends RuntimeException {

    public RecipeAlreadyReportedException(UUID recipeId, UUID reporterId) {
        super("User " + reporterId + " has already reported recipe " + recipeId);
    }
}