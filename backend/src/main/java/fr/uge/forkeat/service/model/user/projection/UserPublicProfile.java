package fr.uge.forkeat.service.model.user.projection;

import java.util.Objects;

public record UserPublicProfile(
        String username,
        String firstName,
        String lastName
) {

    public UserPublicProfile {
        Objects.requireNonNull(username);
        Objects.requireNonNull(firstName);
        Objects.requireNonNull(lastName);
    }
}