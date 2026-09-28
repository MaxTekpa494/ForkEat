package fr.uge.forkeat.service.model.user;

import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UserModerationAction(
    UUID id,
    UUID userId,
    UUID moderatorId,
    UserModerationActionType moderationActionType,
    String justification,
    Instant createdAt,
    Instant updatedAt,
    Instant suspendedUntil,
    UUID relatedReportId
) {
    public UserModerationAction {
      Objects.requireNonNull(id);
      Objects.requireNonNull(userId);
      Objects.requireNonNull(moderatorId);
      Objects.requireNonNull(moderationActionType);
      Objects.requireNonNull(justification);
      Objects.requireNonNull(relatedReportId);
      if (justification.isBlank()) {
        throw new IllegalArgumentException("justification cannot be empty");
      }
    }
}

