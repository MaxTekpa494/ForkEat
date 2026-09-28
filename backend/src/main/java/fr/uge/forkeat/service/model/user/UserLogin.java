package fr.uge.forkeat.service.model.user;

import java.util.Objects;

public record UserLogin(String username, String password) {

	public UserLogin {
		Objects.requireNonNull(username);
		Objects.requireNonNull(password);
	}
}