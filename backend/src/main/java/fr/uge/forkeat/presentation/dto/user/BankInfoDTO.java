package fr.uge.forkeat.presentation.dto.user;

import java.util.UUID;

public record BankInfoDTO(
        UUID userId,
        String bankName,
        String maskedIban,
        String bic
) {
  // LES VERIFS
}
