package fr.uge.forkeat.service.model.superlike;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Promotion(
        UUID id,
        String name,
        Instant startsAt,
        Instant endsAt,
        long priceCents,
        Integer bonusEveryN,  // nullable : pas de mécanisme de bonus
        PromotionStatus status,
        Instant createdAt
) {
    public Promotion {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(startsAt);
        Objects.requireNonNull(endsAt);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
        if (priceCents <= 0) throw new IllegalArgumentException("priceCents must be > 0");
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
        if (bonusEveryN != null && bonusEveryN < 2) {
            throw new IllegalArgumentException("bonusEveryN must be >= 2");
        }
    }

    public boolean isModifiable() {
        return status == PromotionStatus.SCHEDULED;
    }
}
