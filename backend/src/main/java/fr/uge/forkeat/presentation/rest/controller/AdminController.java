package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.user.UserRegisterDTO;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.user.UserRegistrationService;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private final UserRegistrationService userService;

	public AdminController(UserRegistrationService userService) {
		this.userService = Objects.requireNonNull(userService);
	}
}
