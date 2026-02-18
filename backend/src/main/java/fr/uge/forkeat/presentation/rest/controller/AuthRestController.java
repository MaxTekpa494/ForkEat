package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.infrastructure.config.JwtUtils;
import fr.uge.forkeat.presentation.dto.user.GoogleIdTokenRequestDTO;
import fr.uge.forkeat.presentation.dto.user.UserDTO;
import fr.uge.forkeat.presentation.dto.user.UserLoginDTO;
import fr.uge.forkeat.presentation.dto.user.UserRegisterDTO;
import fr.uge.forkeat.presentation.mapper.rest.UserDTOMapper;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.service.exception.RegisterFailureException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.user.GoogleTokenVerificationService;
import fr.uge.forkeat.service.user.UserRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserQueryService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

	private static final Logger logger = LoggerFactory.getLogger(AuthRestController.class);

	private final UserRegistrationService userService;
	private final AuthenticationManager authenticationManager;
	private final JwtUtils jwtUtils;
	private final GoogleTokenVerificationService googleTokenVerificationService;
	private final UserQueryService userQueryService;
	private final AuthenticationPort authPort;

	public AuthRestController(UserRegistrationService userService, AuthenticationManager authenticationManager,
			JwtUtils jwtUtils, GoogleTokenVerificationService googleTokenVerificationService,
			UserQueryService userQueryService, AuthenticationPort authPort) {
		this.userService = userService;
		this.authenticationManager = authenticationManager;
		this.jwtUtils = jwtUtils;
		this.googleTokenVerificationService = googleTokenVerificationService;
		this.userQueryService = userQueryService;
		this.authPort = authPort;
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

	@GetMapping("/me")
	public ResponseEntity<HttpResponse<UserDTO>> me() { // c quoi ce me là ?
		var username = authPort.extractUsername();
		var user = userQueryService.getUserByUsername(username);
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

			var user = userService.registerUserFromOAuth2(
					givenName != null ? givenName : "",
					familyName != null ? familyName : "",
					email,
					AuthMode.GOOGLE);

			var authData = new HashMap<String, String>();
			authData.put("token", jwtUtils.generateToken(user.username()));
			authData.put("type", "Bearer");
			return ResponseEntity.ok(authData);
		} catch (Exception e) {
			logger.error("Google OAuth2 login failed", e);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Google authentication failed");
		}
	}
}
