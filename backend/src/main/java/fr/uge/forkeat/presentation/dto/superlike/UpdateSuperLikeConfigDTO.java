package fr.uge.forkeat.presentation.dto.superlike;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateSuperLikeConfigDTO(
        @NotNull @Min(1) Long priceCents,
        @NotNull @DecimalMin("0.01") @DecimalMax("0.99") BigDecimal earningsRatio
) {}
