package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeReportEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.RecipeReportDetailsView;
import fr.uge.forkeat.service.model.recipe.RecipeReport;
import fr.uge.forkeat.service.model.recipe.projection.RecipeReportDetails;

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

    public static RecipeReportEntity toEntity(RecipeReport report, RecipeEntity recipe, UserEntity reporter, UserEntity reviewedBy) {
        var entity = new RecipeReportEntity();
        entity.setId(report.id());
        entity.setRecipe(recipe);
        entity.setReporter(reporter);
        entity.setReportType(report.reportType());
        entity.setStatus(report.status());
        entity.setJustification(report.justification());
        entity.setReviewedAt(report.reviewedAt());
        entity.setReviewedBy(reviewedBy);
        return entity;
    }

    public static RecipeReportDetails toDomain(RecipeReportDetailsView view) {
        return new RecipeReportDetails(
                view.getId(),
                view.getRecipeId(),
                view.getRecipeTitle(),
                view.getRecipeImageUrl(),
                view.getReporterUsername(),
                view.getReportType(),
                view.getJustification(),
                view.getCreatedAt()
        );
    }
}