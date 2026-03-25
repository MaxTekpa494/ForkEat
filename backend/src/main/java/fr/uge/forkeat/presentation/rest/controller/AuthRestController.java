package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.user.*;
import fr.uge.forkeat.presentation.mapper.rest.UserDTOMapper;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.service.exception.AuthenticationTokenException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.port.GoogleTokenVerificationPort;
import fr.uge.forkeat.service.user.EmailVerificationService;
import fr.uge.forkeat.service.user.UserRegistrationService;
import fr.uge.forkeat.service.user.UserService;
import fr.uge.forkeat.service.user.UserUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

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
	private final GoogleTokenVerificationPort googleTokenVerificationPort;
	private final UserUpdateService userUpdateService;

	public AuthRestController(UserRegistrationService userRegistrationService,
			AuthenticationManager authenticationManager,
			AuthenticationPort authPort,
			UserService userService,
			EmailVerificationService emailVerificationService,
			GoogleTokenVerificationPort googleTokenVerificationPort,
			UserUpdateService userUpdateService) {
		this.userRegistrationService = userRegistrationService;
		this.authenticationManager = authenticationManager;
		this.authPort = authPort;
		this.userService = userService;
		this.emailVerificationService = emailVerificationService;
		this.googleTokenVerificationPort = googleTokenVerificationPort;
		this.userUpdateService = Objects.requireNonNull(userUpdateService);
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
		var user = userRegistrationService.registerUser(UserDTOMapper.toUserRegister(userRegisterDTO));
		var userDTO = UserDTOMapper.toDTO(user);
		return ResponseEntity.ok(new ItemResponse<>(userDTO));
	}

	@PostMapping("/login")
	public ResponseEntity<HttpResponse<AuthTokenDTO>> login(@RequestBody UserLoginDTO userLogin) {
		Objects.requireNonNull(userLogin);
		try {
			authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(userLogin.username(), userLogin.password()));
			return ResponseEntity.ok(new ItemResponse<>(new AuthTokenDTO(authPort.generateToken(userLogin.username()), "Bearer")));
		} catch (AuthenticationException e) {
			throw new AuthenticationTokenException("Invalid username or password");
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
	public ResponseEntity<HttpResponse<AuthTokenDTO>> loginWithGoogle(@RequestBody GoogleIdTokenRequestDTO request) {
		Objects.requireNonNull(request);
		try {
			var googleUserInfo = googleTokenVerificationPort.verify(request.idToken());
			var user = userRegistrationService.registerUserFromOAuth2(
					googleUserInfo.givenName(),
					googleUserInfo.familyName(),
					googleUserInfo.email(),
					AuthMode.GOOGLE);
			return ResponseEntity.ok(new ItemResponse<>(new AuthTokenDTO(authPort.generateToken(user.username()), "Bearer")));
		} catch (AuthenticationTokenException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Google OAuth2 login failed", e);
			throw new AuthenticationTokenException("Google authentication failed");
		}
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<HttpResponse<ForgotPasswordStatusDTO>> forgotPassword(@RequestBody ChangePasswordDTO changePasswordDTO) {
		var userOpt = userService.findByEmail(changePasswordDTO.email());
		if (userOpt.isPresent() && !userOpt.get().emailVerified()) {
			return ResponseEntity.ok(new ItemResponse<>(new ForgotPasswordStatusDTO(false)));
		}
		userOpt.ifPresent(user -> emailVerificationService.sendPasswordChangeCode(user.id(), user.email()));
		return ResponseEntity.ok(new ItemResponse<>(new ForgotPasswordStatusDTO(true)));
	}

	@PostMapping("/forgot-password/confirm-code")
	public ResponseEntity<HttpResponse<Void>> forgotPasswordConfirmCode(@RequestBody ChangePasswordConfirmCodeDTO changePasswordConfirmCodeDTO) {
		userUpdateService.confirmForgotPasswordChange(
				changePasswordConfirmCodeDTO.email(),
				changePasswordConfirmCodeDTO.code(),
				changePasswordConfirmCodeDTO.password(),
				changePasswordConfirmCodeDTO.confirmPassword()
		);
		return ResponseEntity.ok().build();
	}
}