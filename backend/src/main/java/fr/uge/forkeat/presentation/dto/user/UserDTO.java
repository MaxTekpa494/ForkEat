package fr.uge.forkeat.presentation.dto.user;

import java.time.Instant;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String username,
        String firstName,
        String lastName,
        String email,
        String role,
        String status,
        String authMode,
        UUID walletId,
        UUID bankInfoId,
        Instant createdAt,
        Instant updatedAt
) {
  // Les verifs ...
}
