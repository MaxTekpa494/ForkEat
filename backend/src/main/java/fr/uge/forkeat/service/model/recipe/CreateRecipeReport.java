package fr.uge.forkeat.service.model.recipe;

import java.util.Objects;
import java.util.UUID;

public record CreateRecipeReport(
        UUID recipeId,
        String reporterUsername,
        RecipeReportType reportType,
        String justification
) {
    public CreateRecipeReport {
        Objects.requireNonNull(recipeId);
        Objects.requireNonNull(reporterUsername);
        Objects.requireNonNull(reportType);
        Objects.requireNonNull(justification);
        if (justification.isBlank()) {
            throw new IllegalArgumentException("justification cannot be empty");
        }
    }
}