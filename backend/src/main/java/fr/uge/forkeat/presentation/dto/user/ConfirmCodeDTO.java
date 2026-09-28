package fr.uge.forkeat.presentation.dto.user;

import java.util.Objects;

public record ConfirmCodeDTO(String code) {
    public ConfirmCodeDTO {
        Objects.requireNonNull(code);
    }
}