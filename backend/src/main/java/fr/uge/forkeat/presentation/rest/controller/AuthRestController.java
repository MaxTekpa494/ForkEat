package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.infrastructure.config.JwtUtils;
import fr.uge.forkeat.presentation.dto.user.UserDTO;
import fr.uge.forkeat.presentation.dto.user.UserLoginDTO;
import fr.uge.forkeat.presentation.dto.user.UserRegisterDTO;
import fr.uge.forkeat.presentation.mapper.rest.UserDTOMapper;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.service.exception.RegisterFailure;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.user.UserRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

	private final UserRegistrationService userService;
	private final AuthenticationManager authenticationManager;
	private final JwtUtils jwtUtils;

	public AuthRestController(UserRegistrationService userService, AuthenticationManager authenticationManager,
			JwtUtils jwtUtils) {
		this.userService = userService;
		this.authenticationManager = authenticationManager;
		this.jwtUtils = jwtUtils;
	}

	/**
	 * Function corresponding to the endpoint used to register a new user
	 * 
	 * @param userRegisterDTO A DTO which represent the data for the registering of
	 *                        a user
	 * @return the user newly created Must return a DTO instead
	 */
	@PostMapping("/register")
	public ResponseEntity<HttpResponse<UserDTO>> registerUser(@RequestBody UserRegisterDTO userRegisterDTO) {
		Objects.requireNonNull(userRegisterDTO);
        if(userRegisterDTO.password().length() < 8){
            throw new RegisterFailure("The password must have at least 8 characters");
        }
		var user = userService.registerUser(UserDTOMapper.toUserRegister(userRegisterDTO));
		var userDTO = UserDTOMapper.toDTO(user);
		return ResponseEntity.ok(new ItemResponse<>(userDTO));
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody UserLoginDTO userLogin) {
		Objects.requireNonNull(userLogin);
		try {
			authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(userLogin.username(), userLogin.password()));
			var authData = new HashMap<String, String>();
			authData.put("token", jwtUtils.generateToken(userLogin.username()));
			authData.put("type", "Bearer");
			return ResponseEntity.ok(authData);
		} catch (AuthenticationException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
		}
	}
}
