package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.SmartSearchConfigEntity;
import fr.uge.forkeat.service.model.smartsearch.SmartSearchConfig;

public final class SmartSearchConfigEntityMapper {

    private SmartSearchConfigEntityMapper() {}

    public static SmartSearchConfig toDomain(SmartSearchConfigEntity entity) {
        return new SmartSearchConfig(
                entity.getId(),
                entity.getTopK(),
                entity.getCost(),
                entity.getUpdatedAt()
        );
    }
}
