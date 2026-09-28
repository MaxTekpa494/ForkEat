package fr.uge.forkeat.presentation.dto.user;

import java.time.Instant;
import java.util.UUID;

public record WalletDTO(
        UUID id,
        UUID userId,
        Long balance,
        Instant updatedAt
) {
  // Les verifs ...
}
