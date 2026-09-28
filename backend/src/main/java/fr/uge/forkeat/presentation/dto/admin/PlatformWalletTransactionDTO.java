package fr.uge.forkeat.presentation.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record PlatformWalletTransactionDTO(
        UUID id,
        String walletType,
        long amountCents,
        String reason,
        UUID referenceId,
        Instant createdAt
) {}