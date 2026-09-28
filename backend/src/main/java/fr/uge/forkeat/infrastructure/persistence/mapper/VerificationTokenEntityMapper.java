package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.VerificationTokenEntity;
import fr.uge.forkeat.service.model.user.VerificationToken;

import java.util.Objects;

public final class VerificationTokenEntityMapper {

    private VerificationTokenEntityMapper() {}

    public static VerificationToken toDomain(VerificationTokenEntity entity) {
        Objects.requireNonNull(entity);
        return new VerificationToken(
                entity.getId(),
                entity.getUserId(),
                entity.getToken(),
                entity.getType(),
                entity.getNewEmail(),
                entity.getPendingPasswordHash(),
                entity.getExpiresAt(),
                entity.getCreatedAt()
        );
    }

    public static VerificationTokenEntity toEntity(VerificationToken token) {
        Objects.requireNonNull(token);
        var entity = new VerificationTokenEntity();
        entity.setId(token.id());
        entity.setUserId(token.userId());
        entity.setToken(token.token());
        entity.setType(token.type());
        entity.setNewEmail(token.newEmail());
        entity.setPendingPasswordHash(token.pendingPasswordHash());
        entity.setExpiresAt(token.expiresAt());
        return entity;
    }
}
