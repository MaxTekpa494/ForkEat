package fr.uge.forkeat.service.model.user.projection;

import java.util.Objects;

public record UserProfile(
        UserPublicProfile publicProfile,
        UserSocialStats socialStats
) {

    public UserProfile {
        Objects.requireNonNull(publicProfile);
        Objects.requireNonNull(socialStats);
    }
}