package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.CheckProfileUpdateFailure;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserUpdateService {
  private final UserPersistence userPersistence;
  private final UserQueryService userQueryService;
  private final PasswordEncoder passwordEncoder;

  UserUpdateService(UserPersistence userPersistence,
                    UserQueryService userQueryService,
                    PasswordEncoder passwordEncoder) {
    this.userPersistence = Objects.requireNonNull(userPersistence);
    this.userQueryService = Objects.requireNonNull(userQueryService);
    this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
  }

  @Transactional(
          isolation = Isolation.REPEATABLE_READ,
          timeout = 10
  )
  public User updateProfile(String currentUsername, String newUsername, String firstName, String lastName) {
    var user = userQueryService.getUserByUsername(currentUsername);

    if (!newUsername.equals(currentUsername) && userPersistence.existsByUsername(newUsername)) {
      throw new CheckProfileUpdateFailure("Ce nom d'utilisateur est déjà pris");
    }

    var updatedUser = new User(
            user.id(),
            newUsername,
            firstName,
            lastName,
            user.email(),
            user.role(),
            user.status(),
            user.authMode(),
            user.createdAt(),
            Instant.now()
    );

    return userPersistence.updateUser(updatedUser);
  }

  @Transactional(
          isolation = Isolation.REPEATABLE_READ,
          timeout = 10
  )
  public User updateEmail(String username, String newEmail, String currentPassword) {
    var user = userQueryService.getUserByUsername(username);
    // Vérifier le mot de passe actuel pour la sécurité

    if (!userPersistence.checkPassword(username, currentPassword)) {
      throw new CheckProfileUpdateFailure("Incorrect password");
    }

    // Vérifier que le nouvel email n'est pas déjà utilisé
    if (!user.email().equals(newEmail) && userPersistence.existsByEmail(newEmail)) {
      throw new CheckProfileUpdateFailure("Cet email est déjà utilisé");
    }

    var updatedUser = new User(
            user.id(),
            user.username(),
            user.firstName(),
            user.lastName(),
            newEmail,
            user.role(),
            user.status(),
            user.authMode(),
            user.createdAt(),
            Instant.now()
    );

    return userPersistence.updateUser(updatedUser);
  }

  @Transactional(
          isolation = Isolation.REPEATABLE_READ,
          timeout = 10
  )
  public void updatePassword(String username, String currentPassword, String newPassword) {
    Objects.requireNonNull(username);
    Objects.requireNonNull(newPassword);
    Objects.requireNonNull(currentPassword);

    var user = userQueryService.getUserByUsername(username);
    var newEncodedPassword = passwordEncoder.encode(newPassword);
    var currentEncodedPassword = passwordEncoder.encode(currentPassword);

    if (!userPersistence.checkPassword(username, newEncodedPassword)) {
      throw new CheckProfileUpdateFailure("Incorrect current password");
    }

    if(currentEncodedPassword.equals(newEncodedPassword)) {
      throw new CheckProfileUpdateFailure("Passwords are the same");
    }

    if (newPassword.length() < 8) {
      throw new CheckProfileUpdateFailure("New password must be at least 8 characters");
    }

    var updatedUser = new User(
            user.id(),
            user.username(),
            user.firstName(),
            user.lastName(),
            user.email(),
            user.role(),
            user.status(),
            user.authMode(),
            user.createdAt(), Instant.now());
    userPersistence.saveUser (updatedUser, currentEncodedPassword);
  }


  /**
   * Migre un user LOCAL vers OAuth2.
   * Permet à un user qui s'est inscrit avec password
   * de se connecter ensuite avec Google.
   */
  @Transactional(
          isolation = Isolation.READ_COMMITTED,
          timeout = 10
  )
  public User migrateToOAuth2(UUID userId, AuthMode newAuthMode) throws ResourceNotFoundException {
    var user = userQueryService.getUserById(userId);

    var migratedUser = new User(
            user.id(),
            user.username(),
            user.firstName(),
            user.lastName(),
            user.email(),
            user.role(),
            user.status(),
            newAuthMode,
            user.createdAt(),
            Instant.now()
    );

    return userPersistence.saveUser(migratedUser, null); // A VOIR
  }
}
