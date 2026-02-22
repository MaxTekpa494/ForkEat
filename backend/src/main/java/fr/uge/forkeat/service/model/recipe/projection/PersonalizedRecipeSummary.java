package fr.uge.forkeat.service.model.recipe.projection;

import java.util.Objects;

public record PersonalizedRecipeSummary(
        RecipeSummary summary,
        boolean likedByCurrentUser,
        boolean superLikedByCurrentUser
) {

    public PersonalizedRecipeSummary {
        Objects.requireNonNull(summary);
    }
}