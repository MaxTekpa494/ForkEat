package fr.uge.forkeat.service.model;

import fr.uge.forkeat.service.exception.InsufficientFundsException;

import java.time.Instant;
import java.util.UUID;

public record Wallet(
        UUID id,
        Long balance,
        UUID userId,
        Instant updatedAt
) {

    public Wallet addFunds(Long funds) {
        return new Wallet(id, balance + funds, userId, Instant.now());
    }

    public Wallet removeFunds(Long funds){
        if (balance - funds < 0) {
            throw new InsufficientFundsException(balance, balance+funds);
        }

        return new Wallet(id, balance - funds, userId, Instant.now());
    }
}