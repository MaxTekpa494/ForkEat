package fr.uge.forkeat.presentation.dto.superlike;

import fr.uge.forkeat.service.model.superlike.Promotion;

import java.time.Instant;
import java.util.UUID;

public record PromotionDTO(
        UUID id,
        String name,
        Instant startsAt,
        Instant endsAt,
        long priceCents,
        Integer bonusEveryN,
        String status,
        Instant createdAt
) {
    public static PromotionDTO from(Promotion promotion) {
        return new PromotionDTO(
                promotion.id(),
                promotion.name(),
                promotion.startsAt(),
                promotion.endsAt(),
                promotion.priceCents(),
                promotion.bonusEveryN(),
                promotion.status().name(),
                promotion.createdAt()
        );
    }
}
