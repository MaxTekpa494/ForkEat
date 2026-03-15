package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeModerationActionEntity;
import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeModerationActionRepository extends JpaRepository<RecipeModerationActionEntity, UUID> {
    List<RecipeModerationActionEntity> findByRecipeId(UUID recipeId);
    List<RecipeModerationActionEntity> findByModerationActionType(RecipeModerationActionType actionType);
}

