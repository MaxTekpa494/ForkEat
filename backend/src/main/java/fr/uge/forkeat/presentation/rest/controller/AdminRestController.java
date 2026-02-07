package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.user.UserDTO;
import fr.uge.forkeat.presentation.dto.user.UserRegisterDTO;
import fr.uge.forkeat.presentation.mapper.rest.UserDTOMapper;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.service.user.UserRegistrationService;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

	private final UserRegistrationService userService;

	public AdminRestController(UserRegistrationService userService) {
		this.userService = Objects.requireNonNull(userService);
	}

	@PostMapping("/register")
	public ResponseEntity<HttpResponse<UserDTO>> registerModerator(@RequestBody UserRegisterDTO userRegisterDTO) {
		Objects.requireNonNull(userRegisterDTO);
		var user = userService.registerModerator(UserDTOMapper.toUserRegister(userRegisterDTO));
		var userDTO = UserDTOMapper.toDTO(user);
		return ResponseEntity.ok(new ItemResponse<>(userDTO));
	}
}
