package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeModerationActionEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeReportEntity;
import fr.uge.forkeat.service.model.recipe.RecipeModerationAction;

public final class RecipeModerationActionEntityMapper {
    private RecipeModerationActionEntityMapper() {}

    public static RecipeModerationAction toDomain(RecipeModerationActionEntity entity) {
        return new RecipeModerationAction(
                entity.getId(),
                entity.getRecipe().getId(),
                entity.getModerator().getId(),
                entity.getModerationActionType(),
                entity.getJustification(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getRelatedReport() != null ? entity.getRelatedReport().getId() : null
        );
    }

    public static RecipeModerationActionEntity toEntity(RecipeModerationAction action, RecipeEntity recipe, UserEntity moderator, RecipeReportEntity relatedReport) {
        var entity = new RecipeModerationActionEntity();
        entity.setRecipe(recipe);
        entity.setModerator(moderator);
        entity.setModerationActionType(action.moderationActionType());
        entity.setJustification(action.justification());
        entity.setRelatedReport(relatedReport);
        return entity;
    }
}

