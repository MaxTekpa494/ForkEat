package fr.uge.forkeat.service.model.user.projection;

import fr.uge.forkeat.service.model.user.User;

import java.util.Objects;

public record UserAccountDetails(
        User user,
        UserSocialStats socialStats,
        long walletBalance,
        long recipeCount
) {

    public UserAccountDetails {
        Objects.requireNonNull(user);
        Objects.requireNonNull(socialStats);
    }
}