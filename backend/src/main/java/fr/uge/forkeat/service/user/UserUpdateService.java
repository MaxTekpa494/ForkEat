package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.exception.CheckProfileUpdateFailureException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.persistence.UserPersistence;
import fr.uge.forkeat.service.port.PasswordHasherPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
public class UserUpdateService {
  private final UserPersistence userPersistence;
  private final UserService userService;
  private final PasswordHasherPort passwordHasherPort;
  private final EmailVerificationService emailVerificationService;

  UserUpdateService(UserPersistence userPersistence,
                    UserService userService,
                    PasswordHasherPort passwordHasherPort,
                    EmailVerificationService emailVerificationService) {
    this.userPersistence = Objects.requireNonNull(userPersistence);
    this.userService = Objects.requireNonNull(userService);
    this.passwordHasherPort = Objects.requireNonNull(passwordHasherPort);
    this.emailVerificationService = Objects.requireNonNull(emailVerificationService);
  }

  @Transactional(
          isolation = Isolation.REPEATABLE_READ,
          timeout = 10
  )
  public User updateProfile(String currentUsername, String newUsername, String firstName, String lastName) {
    var user = userService.getUserByUsername(currentUsername);

    if (!user.username().equals(newUsername)) {
      if (userPersistence.existsByUsername(newUsername)) {
        throw new CheckProfileUpdateFailureException("Ce nom d'utilisateur est déjà pris");
      }
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
            Instant.now(),
            user.emailVerified()
    );

    return userPersistence.updateUser(updatedUser);
  }

  @Transactional(
          isolation = Isolation.REPEATABLE_READ,
          timeout = 10
  )
  public void requestEmailChange(String username, String newEmail, String currentPassword) {
    requestEmailChange(username, newEmail, currentPassword, null);
  }

  @Transactional(
          isolation = Isolation.REPEATABLE_READ,
          timeout = 10
  )
  public void requestEmailChange(String username, String newEmail, String currentPassword, String newPassword) {
    var user = userService.getUserByUsername(username);

    if (user.authMode() == AuthMode.LOCAL) {
      var storedHash = userPersistence.findPasswordHashByUsername(username);
      if (!passwordHasherPort.matches(currentPassword, storedHash)) {
        throw new CheckProfileUpdateFailureException("Incorrect password");
      }
    }

    if (!user.email().equals(newEmail) && userPersistence.existsByEmail(newEmail)) {
      throw new CheckProfileUpdateFailureException("Cet email est déjà utilisé");
    }

    boolean switchingToLocal = user.authMode() == AuthMode.GOOGLE && newPassword != null;
    if (switchingToLocal) {
      if (newPassword.length() < 8) {
        throw new CheckProfileUpdateFailureException("Le mot de passe doit contenir au moins 8 caractères");
      }
    }

    emailVerificationService.sendEmailChangeCode(user.id(), user.email(), newEmail, switchingToLocal);
  }

  @Transactional(
          isolation = Isolation.REPEATABLE_READ,
          timeout = 10
  )
  public void requestPasswordChange(String username, String currentPassword, String newPassword) {
    Objects.requireNonNull(username);
    Objects.requireNonNull(currentPassword);
    Objects.requireNonNull(newPassword);

    if (newPassword.length() < 8) {
      throw new CheckProfileUpdateFailureException("New password must be at least 8 characters");
    }

    var user = userService.getUserByUsername(username);
    var storedHash = userPersistence.findPasswordHashByUsername(username);

    if (!passwordHasherPort.matches(currentPassword, storedHash)) {
      throw new CheckProfileUpdateFailureException("Incorrect current password");
    }

    if (passwordHasherPort.matches(newPassword, storedHash)) {
      throw new CheckProfileUpdateFailureException("Passwords are the same");
    }

    emailVerificationService.sendPasswordChangeCode(user.id(), user.email());
  }

  @Transactional(
          isolation = Isolation.REPEATABLE_READ,
          timeout = 10
  )
  public void setPasswordForOAuthUser(String username, String newPassword) {
    Objects.requireNonNull(username);
    Objects.requireNonNull(newPassword);

    if (newPassword.length() < 8) {
      throw new CheckProfileUpdateFailureException("New password must be at least 8 characters");
    }

    var user = userService.getUserByUsername(username);

    if (user.authMode() != AuthMode.GOOGLE) {
      throw new CheckProfileUpdateFailureException("This user already has a local password");
    }

    var updatedUser = new User(
            user.id(),
            user.username(),
            user.firstName(),
            user.lastName(),
            user.email(),
            user.role(),
            user.status(),
            AuthMode.LOCAL,
            user.createdAt(),
            Instant.now(),
            user.emailVerified());

    userPersistence.saveUser(updatedUser, passwordHasherPort.hash(newPassword));
  }
}
