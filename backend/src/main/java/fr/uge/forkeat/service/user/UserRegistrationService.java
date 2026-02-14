package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.RegisterFailureException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRegister;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

@Service
public class UserRegistrationService {

  private final Logger logger = LoggerFactory.getLogger(UserRegistrationService.class);
  private final UserPersistence userPersistence;
  private final WalletService walletService;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationService emailVerificationService;

  UserRegistrationService(UserPersistence userPersistence, WalletService walletService,
                          PasswordEncoder passwordEncoder,
                          EmailVerificationService emailVerificationService) {
    this.userPersistence = userPersistence;
    this.walletService = walletService;
    this.passwordEncoder = passwordEncoder;
    this.emailVerificationService = emailVerificationService;
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 15) // ON VA REGARDER EN DETAIL PLUS TARD
  public User registerUser(UserRegister userRegister) {
    Objects.requireNonNull(userRegister);
    if (userPersistence.existsByEmail(userRegister.email())) { // Max ici il vaut mieux crée une Exception
      // personnalisée
      throw new RegisterFailureException("This email is already in use");
    }

    // Vérifier si le username existe déjà
    if (userPersistence.existsByUsername(userRegister.username())) {
      throw new RegisterFailureException("This username is already taken");
    }

    logger.info("Registering user: " + userRegister);
    var user = new User(UUID.randomUUID(),
            userRegister.username(),
            userRegister.firstName(),
            userRegister.lastName(),
            userRegister.email(),
            UserRole.MEMBER,
            UserStatus.ACTIVE,
            AuthMode.LOCAL,
            Instant.now(), Instant.now(),
            false
    );

    var savedUser = userPersistence.saveUser(user, passwordEncoder.encode(userRegister.password()));
    walletService.createWallet(savedUser.id());
    emailVerificationService.sendEmailConfirmation(savedUser.id(), savedUser.email());
    return savedUser;
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 15)
  public User registerUserFromOAuth2(String firstName, String lastName, String email, AuthMode authMode) {
    Objects.requireNonNull(firstName);
    Objects.requireNonNull(lastName);
    Objects.requireNonNull(email);
    Objects.requireNonNull(authMode);

    var existingUserOpt = userPersistence.findByEmail(email);

    if (existingUserOpt.isPresent()) {
      User existingUser = existingUserOpt.get();
      logger.info("User already exists, logging in: {}", email);
      return existingUser;
    }

    var userId = UUID.randomUUID();

    // Générer un username unique depuis l'email
    var username = generateUsername(email);

    var user = new User(userId,
            username,
            firstName,
            lastName, email,
            UserRole.MEMBER,
						UserStatus.ACTIVE,
						authMode,
            Instant.now(),
            Instant.now(),
            true
    );

    var savedUser = userPersistence.saveUser(user, null);
    // Créer le wallet
    walletService.createWallet(savedUser.id());

    return savedUser;
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 15)
  public User registerModerator(UserRegister userRegister) {
    Objects.requireNonNull(userRegister);
    if (userPersistence.existsByEmail(userRegister.email())) {
      throw new RegisterFailureException("This email is already in use");
    }

    if (userPersistence.existsByUsername(userRegister.username())) {
      throw new RegisterFailureException("This username is already taken");
    }

    logger.info("Registering moderator: " + userRegister);
    var user = new User(UUID.randomUUID(),
            userRegister.username(),
            userRegister.firstName(),
            userRegister.lastName(),
            userRegister.email(),
            UserRole.MODERATOR,
            UserStatus.ACTIVE,
            AuthMode.LOCAL,
            Instant.now(), Instant.now(),
            true
    );

    var savedUser = userPersistence.saveUser(user, passwordEncoder.encode(userRegister.password()));
    walletService.createWallet(savedUser.id());
    return user;
  }

  private String generateUsername(String email) {
    var baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");

    if (!userPersistence.existsByUsername(baseUsername)) {
      return baseUsername;
    }

    var username = baseUsername + new Random().nextInt(1000, 9999);
    while (userPersistence.existsByUsername(username)) {
      username = baseUsername + new Random().nextInt(1000, 9999);
    }
    return username;
  }
}
