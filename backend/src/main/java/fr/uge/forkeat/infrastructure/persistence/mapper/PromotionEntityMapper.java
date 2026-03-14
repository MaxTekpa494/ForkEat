package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.PromotionEntity;
import fr.uge.forkeat.service.model.superlike.Promotion;
import fr.uge.forkeat.service.model.superlike.PromotionStatus;

public final class PromotionEntityMapper {

    private PromotionEntityMapper() {}

    public static Promotion toDomain(PromotionEntity entity) {
        return new Promotion(
                entity.getId(),
                entity.getName(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getPriceCents(),
                entity.getBonusEveryN(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    public static PromotionEntity toEntity(Promotion promotion) {
        var entity = new PromotionEntity();
        entity.setId(promotion.id());
        entity.setName(promotion.name());
        entity.setStartsAt(promotion.startsAt());
        entity.setEndsAt(promotion.endsAt());
        entity.setPriceCents(promotion.priceCents());
        entity.setBonusEveryN(promotion.bonusEveryN());
        entity.setStatus(promotion.status());
        entity.setCreatedAt(promotion.createdAt());
        return entity;
    }
}
