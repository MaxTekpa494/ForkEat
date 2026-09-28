package fr.uge.forkeat.presentation.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBankInfoRequestDTO(
        @NotBlank(message = "Bank name cannot be blank")
        String bankName,

        @NotBlank(message = "IBAN cannot be blank")
        @Size(min = 15, max = 34, message = "IBAN must be between 15 and 34 characters")
        @Pattern(regexp = "^[A-Z0-9]+$", message = "IBAN must contain only uppercase letters and digits")
        String iban,

        @NotBlank(message = "BIC cannot be blank")
        @Size(min = 8, max = 11, message = "BIC must be between 8 and 11 characters")
        @Pattern(regexp = "^[A-Z0-9]+$", message = "BIC must contain only uppercase letters and digits")
        String bic
) {
}
