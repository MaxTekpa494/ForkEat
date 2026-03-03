package fr.uge.forkeat.presentation.dto.user;

import java.util.Objects;

public record RequestEmailChangeDTO(String newEmail, String currentPassword, String newPassword, String confirmPassword) {
    public RequestEmailChangeDTO {
        Objects.requireNonNull(newEmail);
        Objects.requireNonNull(currentPassword);
        Objects.requireNonNull(newPassword);
        Objects.requireNonNull(confirmPassword);
    }
}