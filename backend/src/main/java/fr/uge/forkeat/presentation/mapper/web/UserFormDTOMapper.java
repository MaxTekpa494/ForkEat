package fr.uge.forkeat.presentation.mapper.web;

import java.util.Objects;
import fr.uge.forkeat.presentation.web.form.RegisterFormDTO;
import fr.uge.forkeat.service.model.user.UserRegister;

public class UserFormDTOMapper {

	private UserFormDTOMapper() {
	}
	public static UserRegister toUserRegister(RegisterFormDTO registerForm) {
		Objects.requireNonNull(registerForm);
		return new UserRegister(registerForm.getUserName(), registerForm.getFirstName(), registerForm.getLastName(),
				registerForm.getEmail(), registerForm.getPassword());
	}
}
