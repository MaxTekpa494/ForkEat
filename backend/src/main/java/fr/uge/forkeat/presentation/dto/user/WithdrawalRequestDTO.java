package fr.uge.forkeat.presentation.dto.user;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WithdrawalRequestDTO(
        @NotNull(message = "Amount cannot be null")
        @Min(value = 1, message = "Amount must be positive")
        Long amount
) {
}
