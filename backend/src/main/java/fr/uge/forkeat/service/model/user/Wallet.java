package fr.uge.forkeat.service.model.user;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Wallet(UUID id, Long balance, Instant updatedAt) {

    public Wallet{
        Objects.requireNonNull(balance);
        if(balance < 0){
            throw new IllegalArgumentException("Wallet : balance < 0");
        }
    }

    private boolean hasSufficientBalance(long amount) {
        return balance >= amount;
    }

    public Wallet debit(long amount) {
        if (!hasSufficientBalance(amount)) {
            throw new IllegalStateException("Insufficient balance");
        }
        return new Wallet(id, balance - amount, Instant.now());
    }

    public Wallet credit(long amount) {
        return new Wallet(id, balance + amount, Instant.now());
    }
}