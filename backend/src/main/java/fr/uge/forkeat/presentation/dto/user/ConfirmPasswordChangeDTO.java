package fr.uge.forkeat.presentation.dto.user;

import java.util.Objects;

public record ConfirmPasswordChangeDTO(String code, String newPassword, String confirmPassword) {
    public ConfirmPasswordChangeDTO {
        Objects.requireNonNull(code);
        Objects.requireNonNull(newPassword);
        Objects.requireNonNull(confirmPassword);
    }
}