package fr.uge.forkeat.presentation.dto;

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
        BankInfoDTO bankInfo,
        WalletDTO wallet,
        Instant createdAt,
        Instant updatedAt
) {
  // Les verifs ...
}
