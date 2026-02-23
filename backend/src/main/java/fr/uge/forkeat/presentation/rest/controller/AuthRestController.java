package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.user.*;
import fr.uge.forkeat.presentation.mapper.rest.UserDTOMapper;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.service.exception.RegisterFailureException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.port.PasswordHasher;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.GoogleTokenVerificationService;
import fr.uge.forkeat.service.user.UserRegistrationService;
import fr.uge.forkeat.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

	private static final Logger logger = LoggerFactory.getLogger(AuthRestController.class);

	private final UserRegistrationService userRegistrationService;
	private final AuthenticationManager authenticationManager;
	private final AuthenticationPort authPort;
	private final UserService userService;
	private final EmailVerificationService emailVerificationService;
	private final GoogleTokenVerificationService googleTokenVerificationService;
	private final PasswordHasher passwordHasher;

	public AuthRestController(UserRegistrationService userRegistrationService,
			AuthenticationManager authenticationManager,
			AuthenticationPort authPort,
			UserService userService,
			EmailVerificationService emailVerificationService,
			GoogleTokenVerificationService googleTokenVerificationService,
			PasswordHasher passwordHasher) {
		this.userRegistrationService = userRegistrationService;
		this.authenticationManager = authenticationManager;
		this.authPort = authPort;
		this.userService = userService;
		this.emailVerificationService = emailVerificationService;
		this.googleTokenVerificationService = googleTokenVerificationService;
		this.passwordHasher = Objects.requireNonNull(passwordHasher);
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
		if (userRegisterDTO.password().length() < 8) {
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
			authData.put("token", authPort.generateToken(userLogin.username()));
			authData.put("type", "Bearer");
			return ResponseEntity.ok(authData);
		} catch (AuthenticationException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
		}
	}

	@GetMapping("/me")
	public ResponseEntity<HttpResponse<UserDTO>> me() {
		var username = authPort.extractUsername();
		var user = userService.getUserByUsername(username);
		var userDTO = UserDTOMapper.toDTO(user);
		return ResponseEntity.ok(new ItemResponse<>(userDTO));
	}

	@PostMapping("/google-login")
	public ResponseEntity<?> loginWithGoogle(@RequestBody GoogleIdTokenRequestDTO request) {
		Objects.requireNonNull(request);
		try {
			var googleIdToken = googleTokenVerificationService.verify(request.idToken());
			if (googleIdToken == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Google ID token");
			}

			var payload = googleIdToken.getPayload();
			var email = payload.getEmail();
			var givenName = (String) payload.get("given_name");
			var familyName = (String) payload.get("family_name");

			var user = userRegistrationService.registerUserFromOAuth2(
					givenName != null ? givenName : "",
					familyName != null ? familyName : "",
					email,
					AuthMode.GOOGLE);

			var authData = new HashMap<String, String>();
			authData.put("token", authPort.generateToken(user.username()));
			authData.put("type", "Bearer");
			return ResponseEntity.ok(authData);
		} catch (Exception e) {
			logger.error("Google OAuth2 login failed", e);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Google authentication failed");
		}
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<?> forgotPassword(@RequestBody ChangePasswordDTO changePasswordDTO) {
		userService.findByEmail(changePasswordDTO.email())
				.ifPresent(user -> emailVerificationService.sendPasswordChangeCode(user.id(), user.email()));
		return ResponseEntity.ok().build();
	}

	@PostMapping("/forgot-password/confirm-code")
	public ResponseEntity<?> forgotPasswordConfirmCode(@RequestBody ChangePasswordConfirmCodeDTO changePasswordConfirmCodeDTO) {
		if (!changePasswordConfirmCodeDTO.password().equals(changePasswordConfirmCodeDTO.confirmPassword())) {
			throw new RegisterFailureException("Passwords do not match");
		}
		if (changePasswordConfirmCodeDTO.password().length() < 8) {
			throw new RegisterFailureException("The password must have at least 8 characters");
		}
		var user = userService.getUserByEmail(changePasswordConfirmCodeDTO.email());
		emailVerificationService.confirmPasswordChange(user.id(), changePasswordConfirmCodeDTO.code(), passwordHasher.hash(changePasswordConfirmCodeDTO.password()));
		return ResponseEntity.ok().build();
	}
}