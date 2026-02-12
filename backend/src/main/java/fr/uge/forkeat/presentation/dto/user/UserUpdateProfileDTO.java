package fr.uge.forkeat.presentation.dto.user;

import java.util.Objects;

public record UserUpdateProfileDTO(String firstName, String lastName, String username) {

    public UserUpdateProfileDTO {
     // Check ..
        Objects.requireNonNull(firstName);
        Objects.requireNonNull(lastName);
        Objects.requireNonNull(username);
    }
}
