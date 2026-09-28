package fr.uge.forkeat.presentation.dto.smartsearch;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSmartSearchConfigDTO(
        @NotNull @Min(1) Integer topK,
        @NotNull @Min(1) Long cost
) {}
