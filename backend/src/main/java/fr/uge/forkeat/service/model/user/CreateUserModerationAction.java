package fr.uge.forkeat.service.model.user;

import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CreateUserModerationAction(
        UUID userId,
        String moderatorUsername,
        UserModerationActionType moderationActionType,
        String justification,
        Instant suspendedUntil,
        UUID relatedReportId
) {
    public CreateUserModerationAction {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(moderatorUsername);
        Objects.requireNonNull(moderationActionType);
        Objects.requireNonNull(justification);
        Objects.requireNonNull(relatedReportId);
        if (justification.isBlank()) {
            throw new IllegalArgumentException("justification cannot be empty");
        }
    }
}