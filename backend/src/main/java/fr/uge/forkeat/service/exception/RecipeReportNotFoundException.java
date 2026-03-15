package fr.uge.forkeat.service.exception;

import java.util.UUID;

public class RecipeReportNotFoundException extends RuntimeException {
  public RecipeReportNotFoundException(UUID recipeReportId) {
    super("Recipe report not found with id: " + recipeReportId);
  }
}
