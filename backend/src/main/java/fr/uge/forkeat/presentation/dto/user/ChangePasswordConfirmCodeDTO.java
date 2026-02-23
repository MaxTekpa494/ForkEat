package fr.uge.forkeat.presentation.dto.user;

public record ChangePasswordConfirmCodeDTO(String email, String code, String password, String confirmPassword) {
}
