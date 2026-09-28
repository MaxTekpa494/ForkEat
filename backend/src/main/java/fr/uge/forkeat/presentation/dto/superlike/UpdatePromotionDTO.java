package fr.uge.forkeat.presentation.dto.superlike;

import jakarta.validation.constraints.Min;

import java.time.Instant;

public record UpdatePromotionDTO(
        String name,
        Instant startsAt,
        Instant endsAt,
        @Min(1) Long priceCents,
        @Min(2) Integer bonusEveryN
) {}
