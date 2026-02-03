package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRegister;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserRegistrationService {

	private final UserPersistence userPersistence;
	private final WalletService walletService;
	private final PasswordEncoder passwordEncoder;

	UserRegistrationService(UserPersistence userPersistence, WalletService walletService,
			PasswordEncoder passwordEncoder) {
		this.userPersistence = userPersistence;
		this.walletService = walletService;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 15)
	public User registerUser(UserRegister userRegister) {
		Objects.requireNonNull(userRegister);
		if (userPersistence.existsByEmail(userRegister.email())) { // Max ici il vaut mieux crée une Exception
																	// personnalisée
			throw new IllegalArgumentException("This email is already in use");
		}

		// Vérifier si le username existe déjà
		if (userPersistence.existsByUsername(userRegister.username())) {
			throw new IllegalArgumentException("This username is already taken");
		}

		var userId = UUID.randomUUID();

		var user = new User(userId, userRegister.username(), userRegister.firstName(), userRegister.lastName(),
				userRegister.email(), Instant.now(), UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.LOCAL);

		var savedUser = userPersistence.saveUser(user, passwordEncoder.encode(userRegister.password()));

		var wallet = walletService.createWallet(savedUser.id());

		var userWithWallet = new User(savedUser.id(), savedUser.username(), savedUser.firstName(), savedUser.lastName(),
				savedUser.email(), savedUser.password(), savedUser.createdAt(), savedUser.role(), savedUser.status(),
				savedUser.authentificationMode(), wallet.id());

		return userPersistence.saveUser(userWithWallet);
	}

	@Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 15)
	public User registerUserFromOAuth2(String firstName, String lastName, String email, AuthMode authMode,
			String profilePicture) throws ResourceNotFoundException {

		// Vérifier que l'email n'existe pas déjà
		if (userPersistence.existsByEmail(email)) {
			throw new IllegalArgumentException("Cet email est déjà utilisé");
		}

		var userId = UUID.randomUUID();

		// Générer un username unique depuis l'email
		var username = generateUsernameFromEmail(email);

		var user = new User(userId, username, firstName, lastName, email, null, // PAS de password pour OAuth2
				Instant.now(), UserRole.MEMBER, UserStatus.ACTIVE, authMode, // GOOGLE
				null);

		var savedUser = userPersistence.saveUser(user);

		// Créer le wallet
		var wallet = walletService.createWallet(savedUser.id());

		var userWithWallet = new User(savedUser.id(), savedUser.username(), savedUser.firstName(), savedUser.lastName(),
				savedUser.email(), savedUser.password(), savedUser.createdAt(), savedUser.role(), savedUser.status(),
				savedUser.authentificationMode(), wallet.id());

		return userPersistence.saveUser(userWithWallet);
	}

	private String generateUsernameFromEmail(String email) {
		var baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
		var username = baseUsername;
		int counter = 1;

		while (userPersistence.existsByUsername(username)) {
			username = baseUsername + counter++;
		}

		return username;
	}
}
