package fr.uge.forkeat.service.model.user.projection;

import java.util.Objects;

public record PersonalizedUserProfile(
        UserProfileWithRecipes profileWithRecipes,
        boolean followedByCurrentUser
) {
    public PersonalizedUserProfile {
        Objects.requireNonNull(profileWithRecipes);
    }
}