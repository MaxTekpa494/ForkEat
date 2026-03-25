package fr.uge.forkeat.service.model.user;

import fr.uge.forkeat.service.model.AuthMode;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record User(UUID id, String username, String firstName, String lastName, String email, UserRole role,
                   UserStatus status, AuthMode authMode, Instant createdAt, Instant updatedAt,
                   boolean emailVerified) {

  public User {
    Objects.requireNonNull(id);
    Objects.requireNonNull(username);
    Objects.requireNonNull(firstName);
    Objects.requireNonNull(lastName);
    Objects.requireNonNull(email);
    Objects.requireNonNull(role);
    Objects.requireNonNull(status);
    Objects.requireNonNull(authMode);
  }

  public boolean isActive() {
    return status == UserStatus.ACTIVE;
  }

  public boolean isAdmin() {
    return role == UserRole.ADMIN;
  }

  public User ban() {
    return new User(id, username, firstName, lastName, email, role, UserStatus.BANNED, authMode, createdAt, Instant.now(), emailVerified);
  }

  public User suspend() {
    return new User(id, username, firstName, lastName, email, role, UserStatus.SUSPENDED, authMode, createdAt, Instant.now(), emailVerified);
  }

  public User unban() {
    return new User(id, username, firstName, lastName, email, role, UserStatus.ACTIVE, authMode, createdAt, Instant.now(), emailVerified);
  }

  public User promoteToModerator() {
    return new User(id, username, firstName, lastName, email, UserRole.MODERATOR, status, authMode, createdAt, Instant.now(), emailVerified);
  }

}
