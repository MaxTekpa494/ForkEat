package fr.uge.forkeat.presentation.dto.user;

public record BankInfoDTO(
        String bankName,
        String maskedIban,
        String bic
) {
  // LES VERIFS
}
