package fr.uge.forkeat.service.model.superlike;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SuperLikeConfig(UUID id, long priceCents, BigDecimal earningsRatio, Instant updatedAt) {
    public SuperLikeConfig {
        Objects.requireNonNull(id);
        Objects.requireNonNull(earningsRatio);
        Objects.requireNonNull(updatedAt);
        if (priceCents <= 0) throw new IllegalArgumentException("priceCents must be > 0");
        if (earningsRatio.compareTo(BigDecimal.ZERO) <= 0 || earningsRatio.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("earningsRatio must be in (0, 1)");
        }
    }
}
