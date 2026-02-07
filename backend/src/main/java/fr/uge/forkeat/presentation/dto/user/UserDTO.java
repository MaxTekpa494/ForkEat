package fr.uge.forkeat.presentation.dto.user;

import java.time.Instant;
import java.util.UUID;

public record UserDTO(
        String username,
        String firstName,
        String lastName,
        String email,
        String role,
        String status,
        String authMode,
        Instant createdAt,
        Instant updatedAt
) {
  // Les verifs ...
}
