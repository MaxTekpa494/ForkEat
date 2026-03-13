package fr.uge.forkeat.service.model.recipe;

import fr.uge.forkeat.service.model.ReportStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RecipeReport(
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
    public RecipeReport {
        Objects.requireNonNull(id);
        Objects.requireNonNull(recipeId);
        Objects.requireNonNull(reportType);
        Objects.requireNonNull(status);
        Objects.requireNonNull(justification);
        Objects.requireNonNull(reviewedAt);
        if (justification.isBlank()) {
            throw new IllegalArgumentException("justification cannot be empty");
        }
    }
}