package fr.uge.forkeat.presentation.dto.user;

import fr.uge.forkeat.service.model.transaction.TransactionStatus;
import fr.uge.forkeat.service.model.transaction.TransactionType;

import java.time.Instant;
import java.util.UUID;

public record TransactionDTO(
        UUID id,
        UUID walletSourceId,
        UUID walletDestinationId,
        Long amount,
        TransactionType type,
        Instant createdAt,
        String stripeTransactionID,
        TransactionStatus status
) {}
