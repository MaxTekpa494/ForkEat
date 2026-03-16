package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.SuperLikeConfigEntity;
import fr.uge.forkeat.service.model.superlike.SuperLikeConfig;

public final class SuperLikeConfigEntityMapper {

    private SuperLikeConfigEntityMapper() {}

    public static SuperLikeConfig toDomain(SuperLikeConfigEntity entity) {
        return new SuperLikeConfig(
                entity.getId(),
                entity.getPriceCents(),
                entity.getEarningsRatio(),
                entity.getUpdatedAt()
        );
    }
}
