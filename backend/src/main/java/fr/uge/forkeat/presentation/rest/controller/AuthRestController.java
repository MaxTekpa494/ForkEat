package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.infrastructure.config.JwtUtils;
import fr.uge.forkeat.presentation.dto.user.*;
import fr.uge.forkeat.presentation.mapper.rest.UserDTOMapper;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.service.exception.RegisterFailureException;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.UserService;
import fr.uge.forkeat.service.user.UserRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

	private final UserRegistrationService userRegistrationService;
	private final AuthenticationManager authenticationManager;
	private final JwtUtils jwtUtils;
	private final UserService userService;
	private final EmailVerificationService emailVerificationService;

	public AuthRestController(UserRegistrationService userRegistrationService, AuthenticationManager authenticationManager,
                              JwtUtils jwtUtils, UserService userService, EmailVerificationService emailVerificationService) {
		this.userRegistrationService = userRegistrationService;
		this.authenticationManager = authenticationManager;
		this.jwtUtils = jwtUtils;
		this.userService = userService;
		this.emailVerificationService = emailVerificationService;
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
            throw new RegisterFailureException("The password must have at least 8 characters");
        }
		var user = userRegistrationService.registerUser(UserDTOMapper.toUserRegister(userRegisterDTO));
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

	@PostMapping("/forgot-password")
	public ResponseEntity<?> forgotPassword(@RequestBody ChangePasswordDTO changePasswordDTO) {
		if(changePasswordDTO.password().length() < 8){
			throw new RegisterFailureException("The password must have at least 8 characters");
		}
		var user = userService.getUserByEmail(changePasswordDTO.email());
		emailVerificationService.sendPasswordChangeCode(user.id(), user.email(), changePasswordDTO.password());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/forgot-password/confirm-code")
	public ResponseEntity<?> forgotPasswordConfirmCode(@RequestBody ChangePasswordConfirmCodeDTO changePasswordConfirmCodeDTO) {
		var user = userService.getUserByEmail(changePasswordConfirmCodeDTO.email());
		emailVerificationService.confirmPasswordChange(user.id(), changePasswordConfirmCodeDTO.code());
		return ResponseEntity.ok().build();
	}
}
