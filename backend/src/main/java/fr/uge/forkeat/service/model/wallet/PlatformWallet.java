package fr.uge.forkeat.service.model.wallet;

import java.time.Instant;
import java.util.UUID;

public record PlatformWallet(UUID id, PlatformWalletType type, long balance, Instant updatedAt) {
}
