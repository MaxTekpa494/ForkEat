package fr.uge.forkeat.service.model.recipe.projection;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RecipeSummary(
        UUID id,
        String title,
        String summary,
        String imageUrl,
        int preparationMinutes,
        Instant createdAt,
        long likeCount,
        long superLikeCount
) {

    public RecipeSummary {
        Objects.requireNonNull(id);
        Objects.requireNonNull(title);
        Objects.requireNonNull(summary);
        if (preparationMinutes < 0) {
            throw new IllegalArgumentException("preparationMinutes cannot be negative");
        }
        if (likeCount < 0) {
            throw new IllegalArgumentException("likeCount cannot be negative");
        }
        if (superLikeCount < 0) {
            throw new IllegalArgumentException("superLikeCount cannot be negative");
        }
    }
}