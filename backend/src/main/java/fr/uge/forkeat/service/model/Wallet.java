package fr.uge.forkeat.service.model;

import java.time.Instant;

public record Wallet(
        Long id,
        Long balance,
        Long userId,
        Instant updatedAt
) {
    // (Puisque le record est immuable, on ne peut pas faire setBalance les gars)
    public Wallet withBalance(Long newBalance) {
        return new Wallet(id, newBalance, userId, Instant.now());
    }
}