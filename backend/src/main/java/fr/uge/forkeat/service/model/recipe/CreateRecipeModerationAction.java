package fr.uge.forkeat.service.model.recipe;

import java.util.Objects;
import java.util.UUID;

public record CreateRecipeModerationAction(
        UUID recipeId,
        String moderatorUsername,
        RecipeModerationActionType moderationActionType,
        String justification
) {
    public CreateRecipeModerationAction {
        Objects.requireNonNull(recipeId);
        Objects.requireNonNull(moderatorUsername);
        Objects.requireNonNull(moderationActionType);
        Objects.requireNonNull(justification);
        if (justification.isBlank() && moderationActionType == RecipeModerationActionType.REJECTED) {
            throw new IllegalArgumentException("justification cannot be empty on rejections");
        }
    }
}