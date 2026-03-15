package fr.uge.forkeat.presentation.dto.superlike;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreatePromotionDTO(
        @NotBlank String name,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @NotNull @Min(1) Long priceCents,
        @Min(2) Integer bonusEveryN  // nullable : pas de bonus ; min 2 pour garantir la rentabilité
) {}
