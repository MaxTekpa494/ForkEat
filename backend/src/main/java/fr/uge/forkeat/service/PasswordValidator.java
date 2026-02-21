package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.RegisterFailureException;

public class PasswordValidator {

    private PasswordValidator() {}

    /**
     * Validates that a password meets complexity requirements:
     * - At least 8 characters
     * - At least 1 uppercase letter
     * - At least 1 digit
     *
     * @throws RegisterFailureException if the password does not meet requirements
     */
    public static void validate(String password) {
        if (password == null || password.length() < 8) {
            throw new RegisterFailureException("Le mot de passe doit contenir au moins 8 caractères");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new RegisterFailureException("Le mot de passe doit contenir au moins une majuscule");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new RegisterFailureException("Le mot de passe doit contenir au moins un chiffre");
        }
    }
}
