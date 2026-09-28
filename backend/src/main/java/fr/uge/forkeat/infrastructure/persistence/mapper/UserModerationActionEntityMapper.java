package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserModerationActionEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserReportEntity;
import fr.uge.forkeat.service.model.user.UserModerationAction;

import java.time.Instant;

public final class UserModerationActionEntityMapper {
    private UserModerationActionEntityMapper() {}

    public static UserModerationAction toDomain(UserModerationActionEntity entity) {
        return new UserModerationAction(
                entity.getId(),
                entity.getUser().getId(),
                entity.getModerator().getId(),
                entity.getModerationActionType(),
                entity.getJustification(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getSuspendedUntil(),
                entity.getRelatedReport().getId()
        );
    }

    public static UserModerationActionEntity toEntity(UserModerationAction action, UserEntity user, UserEntity moderator, UserReportEntity relatedReport) {
        var entity = new UserModerationActionEntity();
        entity.setUser(user);
        entity.setModerator(moderator);
        entity.setModerationActionType(action.moderationActionType());
        entity.setJustification(action.justification());
        entity.setSuspendedUntil(action.suspendedUntil());
        entity.setRelatedReport(relatedReport);
        return entity;
    }
}

