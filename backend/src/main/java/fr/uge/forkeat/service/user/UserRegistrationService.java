package fr.uge.forkeat.service.user;

import fr.uge.forkeat.presentation.mapper.web.UserFormDTOMapper;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
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
import java.util.random.RandomGenerator;

@Service
public class UserRegistrationService {

  private final Logger logger = LoggerFactory.getLogger(UserRegistrationService.class);
  private final UserPersistence userPersistence;
  private final WalletService walletService;
  private final PasswordEncoder passwordEncoder;

  UserRegistrationService(UserPersistence userPersistence, WalletService walletService,
                          PasswordEncoder passwordEncoder) {
    this.userPersistence = userPersistence;
    this.walletService = walletService;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 15) // ON VA REGARDER EN DETAIL PLUS TARD
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

    logger.info("Registering user: " + userRegister);
    var user = new User(UUID.randomUUID(),
            userRegister.username(),
            userRegister.firstName(),
            userRegister.lastName(),
            userRegister.email(),
            UserRole.MEMBER,
            UserStatus.ACTIVE,
            AuthMode.LOCAL,
            Instant.now(), Instant.now()
    );

    var savedUser = userPersistence.saveUser(user, passwordEncoder.encode(userRegister.password()));
    walletService.createWallet(savedUser.id());
    return user;
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 15)
  public User registerUserFromOAuth2(String firstName, String lastName, String email, AuthMode authMode) {
    Objects.requireNonNull(firstName);
    Objects.requireNonNull(lastName);
    Objects.requireNonNull(email);
    Objects.requireNonNull(authMode);
    // Vérifier que l'email n'existe pas déjà
    if (userPersistence.existsByEmail(email)) {
      throw new IllegalArgumentException("Cet email est déjà utilisé");
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
            Instant.now(),// GOOGLE
            Instant.now());

    var savedUser = userPersistence.saveUser(user, null);
    // Créer le wallet
    walletService.createWallet(savedUser.id());

    return savedUser;
  }

  private String generateUsername(String email) {
    var baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
    var username = baseUsername + new Random().nextLong();
    while (userPersistence.existsByUsername(username)) { // SI C'EST FAIT 1 FOIS C'EST BON.
      username = baseUsername + new Random().nextLong();
    }
    return username;
  }
}
