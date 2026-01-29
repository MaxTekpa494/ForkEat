package fr.uge.forkeat.presentation.dto.user;

import java.time.Instant;
import java.util.UUID;

public record WalletDTO(
        UUID id,
        Long balance,
        Instant updatedAt
) {
  // Les verifs ...
}
