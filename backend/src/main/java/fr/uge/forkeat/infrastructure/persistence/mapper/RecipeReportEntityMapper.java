package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeReportEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.service.model.recipe.RecipeReport;

public final class RecipeReportEntityMapper {

    private RecipeReportEntityMapper() {}

    public static RecipeReport toDomain(RecipeReportEntity entity) {
        return new RecipeReport(
                entity.getId(),
                entity.getRecipe().getId(),
                entity.getReporter() != null ? entity.getReporter().getId() : null,
                entity.getReportType(),
                entity.getStatus(),
                entity.getJustification(),
                entity.getCreatedAt(),
                entity.getReviewedAt(),
                entity.getReviewedBy() != null ? entity.getReviewedBy().getId() : null
        );
    }

    public static RecipeReportEntity toEntity(RecipeReport report, RecipeEntity recipe, UserEntity reporter) {
        var entity = new RecipeReportEntity();
        entity.setRecipe(recipe);
        entity.setReporter(reporter);
        entity.setReportType(report.reportType());
        entity.setStatus(report.status());
        entity.setJustification(report.justification());
        return entity;
    }
}