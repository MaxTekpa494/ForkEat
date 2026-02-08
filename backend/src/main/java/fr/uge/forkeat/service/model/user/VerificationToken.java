package fr.uge.forkeat.service.model.user;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record VerificationToken(UUID id, UUID userId, String token, VerificationTokenType type,
                                String payload, Instant expiresAt, Instant createdAt) {

    public VerificationToken {
        Objects.requireNonNull(id);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(token);
        Objects.requireNonNull(type);
        Objects.requireNonNull(expiresAt);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
