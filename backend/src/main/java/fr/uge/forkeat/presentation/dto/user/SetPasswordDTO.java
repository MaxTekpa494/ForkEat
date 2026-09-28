package fr.uge.forkeat.presentation.dto.user;

import java.util.Objects;

public record SetPasswordDTO(String newPassword, String confirmPassword) {
    public SetPasswordDTO {
        Objects.requireNonNull(newPassword);
        Objects.requireNonNull(confirmPassword);
    }
}