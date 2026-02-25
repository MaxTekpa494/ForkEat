package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.PasswordValidator;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.RegisterFailureException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRegister;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.persistence.UserPersistence;
import fr.uge.forkeat.service.port.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserRegistrationService {

    private final Logger logger = LoggerFactory.getLogger(UserRegistrationService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final UserPersistence userPersistence;
    private final WalletService walletService;
    private final PasswordHasher passwordHasher;
    private final EmailVerificationService emailVerificationService;

    UserRegistrationService(UserPersistence userPersistence, WalletService walletService,
                            PasswordHasher passwordHasher,
                            EmailVerificationService emailVerificationService) {
        this.userPersistence = userPersistence;
        this.walletService = walletService;
        this.passwordHasher = passwordHasher;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 15)
    public User registerUser(UserRegister userRegister) {
        Objects.requireNonNull(userRegister);
        if (userPersistence.existsByEmail(userRegister.email())) {
            throw new RegisterFailureException("This email is already in use");
        }

        if (userPersistence.existsByUsername(userRegister.username())) {
            throw new RegisterFailureException("This username is already taken");
        }

        PasswordValidator.validate(userRegister.password());

        logger.info("Registering user: {}", userRegister);
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

        var savedUser = userPersistence.saveUser(user, passwordHasher.hash(userRegister.password()));
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

        logger.info("Registering moderator: {}", userRegister);
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

        var savedUser = userPersistence.saveUser(user, passwordHasher.hash(userRegister.password()));
        walletService.createWallet(savedUser.id());
        return savedUser;
    }

  @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 15)
  public User registerAdmin(UserRegister userRegister) {
    Objects.requireNonNull(userRegister);
    if (userPersistence.existsByEmail(userRegister.email())) {
      throw new RegisterFailureException("This email is already in use");
    }

    if (userPersistence.existsByUsername(userRegister.username())) {
      throw new RegisterFailureException("This username is already taken");
    }

    logger.info("Registering admin with username: {}", userRegister.username());
    var user = new User(UUID.randomUUID(),
            userRegister.username(),
            userRegister.firstName(),
            userRegister.lastName(),
            userRegister.email(),
            UserRole.ADMIN,
            UserStatus.ACTIVE,
            AuthMode.LOCAL,
            Instant.now(), Instant.now(),
            true // email verified by default
    );

    // Admin has no personal wallet per platform specification
    return userPersistence.saveUser(user, passwordHasher.hash(userRegister.password()));
  }

  private String generateUsername(String email) {
        var baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");

        if (!userPersistence.existsByUsername(baseUsername)) {
            return baseUsername;
        }

        var username = baseUsername + SECURE_RANDOM.nextInt(1000, 9999);
        while (userPersistence.existsByUsername(username)) {
            username = baseUsername + SECURE_RANDOM.nextInt(1000, 9999);
        }
        return username;
    }
}