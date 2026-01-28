package fr.uge.forkeat.presentation.dto;

public record BankInfoDTO(
        String bankName,
        String maskedIban,
        String bic
) {}
