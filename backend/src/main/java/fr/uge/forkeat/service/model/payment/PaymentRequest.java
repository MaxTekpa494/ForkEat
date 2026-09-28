package fr.uge.forkeat.service.model.payment;

import java.util.UUID;

public record PaymentRequest(UUID userId, String email, Long amount, String currency, String source) {
    public PaymentRequest {
        if (amount < 100L) { // 1€
            throw new IllegalArgumentException("Le montant doit être supérieur ou égal à 1 euro");
        }
    }
}