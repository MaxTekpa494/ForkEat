package fr.uge.forkeat.service.model.recipe;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RecipeModerationAction(
    UUID id,
    UUID recipeId,
    UUID moderatorId,
    RecipeModerationActionType moderationActionType,
    String justification,
    Instant createdAt,
    Instant updatedAt,
    UUID relatedReportId
) {
    public RecipeModerationAction {
      Objects.requireNonNull(id);
      Objects.requireNonNull(recipeId);
      Objects.requireNonNull(moderatorId);
      Objects.requireNonNull(moderationActionType);
      Objects.requireNonNull(justification);
      if (justification.isBlank() && moderationActionType == RecipeModerationActionType.REJECTED) {
        throw new IllegalArgumentException("justification cannot be empty on rejections");
      }
    }
}

