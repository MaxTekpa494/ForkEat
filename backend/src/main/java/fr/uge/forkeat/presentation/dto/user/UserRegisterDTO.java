package fr.uge.forkeat.presentation.dto.user;


public record UserRegisterDTO(String username, String firstName, String lastName, String password, String email) {
  // LES VERIFICATIONS : objects.requireNonNull ou les decoration
}
