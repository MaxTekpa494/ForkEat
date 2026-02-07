package fr.uge.forkeat.service.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record Transaction(UUID walletSourceId, UUID walletDestinationId, Long amount, TransactionType type,
		Instant createdAt, String stripeTransactionID) {
	// LES VERIFS
}