package fr.uge.forkeat.service.model.user;

import java.util.Objects;

public record UserRegister(String username, String firstName, String lastName, String password, String email) {
  public UserRegister{
    Objects.requireNonNull(username);
    Objects.requireNonNull(firstName);
    Objects.requireNonNull(lastName);
    Objects.requireNonNull(password);
    Objects.requireNonNull(email);
  }
}
