package fr.uge.forkeat.service.model.user;

import fr.uge.forkeat.service.model.AuthMode;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record User(UUID id, String username, String firstName, String lastName, String email, UserRole role,
		UserStatus status, AuthMode authMode, UUID walletId, UUID bankInfoId, Instant createdAt, Instant updatedAt) {

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

	public boolean hasBankInfo() {
		return bankInfoId != null;
	}

	public boolean hasWallet() {
		return walletId != null;
	}

}
