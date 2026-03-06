package fr.uge.forkeat.presentation.dto.recipe;

import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.recipe.RecipeReport;
import fr.uge.forkeat.service.model.recipe.RecipeReportType;

import java.time.Instant;
import java.util.UUID;

public record RecipeReportDTO(
        UUID id,
        UUID recipeId,
        UUID reporterId,
        RecipeReportType reportType,
        ReportStatus status,
        String justification,
        Instant createdAt,
        Instant reviewedAt,
        UUID reviewedById
) {
    public static RecipeReportDTO from(RecipeReport report) {
        return new RecipeReportDTO(
                report.id(),
                report.recipeId(),
                report.reporterId(),
                report.reportType(),
                report.status(),
                report.justification(),
                report.createdAt(),
                report.reviewedAt(),
                report.reviewedById()
        );
    }
}