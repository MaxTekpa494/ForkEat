package fr.uge.forkeat.service.model;

import java.time.LocalDateTime;

public record User(
    String username,
    String firstName,
    String lastName,
    String email,
    String password,
    LocalDateTime createdAt,
    UserRole role,
    UserStatus status,
    AuthMode authentificationMode,
    Long walletId
) {

}