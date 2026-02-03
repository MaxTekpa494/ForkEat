package fr.uge.forkeat.presentation.rest.dto;

public record UserLoginDTO(String username, String password) {
  // LES VERIFICATIONS : objects.requireNonNull ou les decoration
}
