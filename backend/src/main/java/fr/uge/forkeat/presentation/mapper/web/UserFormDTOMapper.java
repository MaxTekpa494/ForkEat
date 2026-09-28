package fr.uge.forkeat.presentation.mapper.web;

import fr.uge.forkeat.presentation.web.form.RegisterFormDTO;
import fr.uge.forkeat.service.model.user.UserRegister;

import java.util.Objects;

public class UserFormDTOMapper {

	private UserFormDTOMapper() {
	}

	public static UserRegister toUserRegister(RegisterFormDTO registerForm) {
		Objects.requireNonNull(registerForm);
		return new UserRegister(registerForm.getUserName(), registerForm.getFirstName(), registerForm.getLastName(),
						registerForm.getPassword(), registerForm.getEmail());
	}
}
