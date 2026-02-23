package fr.uge.forkeat.presentation.dto.user;

import java.util.UUID;

public record BankInfoResponseDTO(
        UUID userId,
        String bankName,
        String externalAccountId
) {
}