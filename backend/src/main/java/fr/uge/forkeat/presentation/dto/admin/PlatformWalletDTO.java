package fr.uge.forkeat.presentation.dto.admin;

import java.time.Instant;

public record PlatformWalletDTO(String type, long balance, Instant updatedAt) {
}
