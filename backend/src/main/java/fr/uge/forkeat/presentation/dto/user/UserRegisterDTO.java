package fr.uge.forkeat.presentation.rest.dto;


public record UserRegisterDTO(String username, String firstName, String lastName, String password, String email) {
  // LES VERIFICATIONS : objects.requireNonNull ou les decoration
}
