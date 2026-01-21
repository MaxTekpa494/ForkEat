package fr.uge.forkeat.service.model;

import java.time.Instant;
import java.util.UUID;

public record Wallet(
        UUID id,
        Long balance,
        UUID userId,
        Instant updatedAt
) {
    // (Puisque le record est immuable, on ne peut pas faire setBalance les gars)
    public Wallet withBalance(Long newBalance) {
        return new Wallet(id, newBalance, userId, Instant.now());
    }
}