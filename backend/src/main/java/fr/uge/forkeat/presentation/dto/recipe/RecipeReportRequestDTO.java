package fr.uge.forkeat.presentation.dto.recipe;

import fr.uge.forkeat.service.model.recipe.RecipeReportType;

public record RecipeReportRequestDTO(RecipeReportType reportType, String justification) {
    public RecipeReportRequestDTO {
        if (reportType == null) {
            throw new IllegalArgumentException("reportType is required");
        }
        if (justification == null || justification.isBlank()) {
            throw new IllegalArgumentException("justification cannot be empty");
        }
    }
}