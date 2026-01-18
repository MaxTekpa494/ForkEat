package fr.uge.forkeat.service.model;

import java.time.Instant;
import java.time.LocalDateTime;

public record Transaction(
    Long walletSourceId,
    Long walletDestinationId,
    Long amount,
    TransactionType type,
    Instant createdAt,
    String stripeTransactionId
) {}