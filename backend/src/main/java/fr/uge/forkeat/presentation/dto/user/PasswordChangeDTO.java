package fr.uge.forkeat.presentation.dto.user;

import java.util.Objects;

public record PasswordChangeDTO (String currentPassword, String newPassword, String confirmPassword){
    public PasswordChangeDTO {
        Objects.requireNonNull(currentPassword);
        Objects.requireNonNull(newPassword);
        Objects.requireNonNull(confirmPassword);
    }
}
