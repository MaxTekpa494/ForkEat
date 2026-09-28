package fr.uge.forkeat.service.model.recipe.projection;

import java.time.Instant;
import java.util.UUID;

public record RecipeReportDetails(
    UUID id,
    UUID recipeId,
    String recipeTitle,
    String recipeImageUrl,
    String reporterUsername,
    String reportType,
    String justification,
    Instant createdAt
) {}

