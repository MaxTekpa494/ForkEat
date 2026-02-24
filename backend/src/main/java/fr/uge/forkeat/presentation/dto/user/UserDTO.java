package fr.uge.forkeat.presentation.dto.user;

import java.time.Instant;

public record UserDTO(
        String username,
        String firstName,
        String lastName,
        String email,
        String role,
        String status,
        String authMode,
        boolean emailVerified,
        Instant createdAt,
        Instant updatedAt
) {
  // Les verifs ...
}
