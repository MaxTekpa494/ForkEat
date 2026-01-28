package fr.uge.forkeat.presentation.rest.dto;

import java.util.UUID;

public record PaymentRequestDto(UUID userId, String email, Long amount, String currency) {
    public PaymentRequestDto {
        if (amount < 100L) { // 1€
            throw new IllegalArgumentException("Le montant doit être supérieur ou égal à 1 euro");
        }
    }
}