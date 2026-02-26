package fr.uge.forkeat.service.model.wallet;

import fr.uge.forkeat.service.exception.InsufficientFundsException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Wallet(UUID id, UUID userId, long balance, Instant updatedAt) {

  public Wallet {
    Objects.requireNonNull(userId);
    if (balance < 0) {
      throw new IllegalArgumentException("Wallet : balance < 0");
    }
  }

  private boolean hasSufficientBalance(long amount) {
    return balance >= amount;
  }

  public Wallet debit(long amount) {
    if (!hasSufficientBalance(amount)) {
      throw new InsufficientFundsException(balance, amount + balance);
    }
    return new Wallet(id, userId, balance - amount, Instant.now());
  }

  public Wallet credit(long amount) {
    if (amount < 0) {
      throw new InsufficientFundsException(balance, amount);
    }
    return new Wallet(id, userId, balance + amount, Instant.now());
  }
}