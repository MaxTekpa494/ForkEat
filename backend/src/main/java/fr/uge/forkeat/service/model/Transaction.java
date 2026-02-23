package fr.uge.forkeat.service.model;

import java.time.Instant;
import java.util.UUID;

public record Transaction(UUID id, UUID walletSourceId, UUID walletDestinationId, Long amount, TransactionType type,
		Instant createdAt, String stripeTransactionID, TransactionStatus status) {
	// LES VERIFS
}