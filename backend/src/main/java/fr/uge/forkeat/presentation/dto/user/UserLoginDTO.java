package fr.uge.forkeat.presentation.dto.user;
import java.util.Objects;

public record UserLoginDTO(String username, String password) {
  // LES VERIFICATIONS : objects.requireNonNull ou les decoration

  public UserLoginDTO{
    Objects.requireNonNull(username);
    Objects.requireNonNull(password);
  }
}
