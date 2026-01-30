package fr.uge.forkeat.service.model;

import java.time.Instant;
import java.util.UUID;

public record User(
    UUID id,
    String username,
    String firstName,
    String lastName,
    String email,
    String password,
    Instant createdAt,
    UserRole role,
    UserStatus status,
    AuthMode authentificationMode,
    UUID walletId
) {

}