package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.service.user.UserRegistrationService;

import java.util.Objects;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

	private final UserRegistrationService userService;

	public AdminRestController(UserRegistrationService userService) {
		this.userService = Objects.requireNonNull(userService);
	}
}
