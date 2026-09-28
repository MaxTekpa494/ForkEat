package fr.uge.forkeat.service.model.wallet;

import java.time.Instant;
import java.util.UUID;

public record PlatformWalletTransaction(
        UUID id,
        PlatformWalletType walletType,
        long amountCents,        // signé : positif = crédit, négatif = débit
        String reason,           // SUPER_LIKE_EARNED | SUPER_LIKE_REDISTRIBUTION | BONUS_FINANCED
        UUID referenceId,        // UUID de la recette concernée
        Instant createdAt
) {}
