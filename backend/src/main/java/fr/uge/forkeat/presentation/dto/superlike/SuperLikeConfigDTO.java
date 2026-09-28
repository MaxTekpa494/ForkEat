package fr.uge.forkeat.presentation.dto.superlike;

import fr.uge.forkeat.service.model.superlike.SuperLikeConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SuperLikeConfigDTO(UUID id, long priceCents, BigDecimal earningsRatio, Instant updatedAt) {
    public static SuperLikeConfigDTO from(SuperLikeConfig config) {
        return new SuperLikeConfigDTO(config.id(), config.priceCents(), config.earningsRatio(), config.updatedAt());
    }
}
