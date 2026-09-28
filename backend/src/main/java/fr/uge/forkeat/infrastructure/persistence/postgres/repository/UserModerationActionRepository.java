package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeModerationActionEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserModerationActionEntity;
import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;
import fr.uge.forkeat.service.model.user.UserModerationActionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserModerationActionRepository extends JpaRepository<UserModerationActionEntity, UUID> {
    List<UserModerationActionEntity> findByUserId(UUID userId);
    List<UserModerationActionEntity> findByModerationActionType(UserModerationActionType actionType);
}

