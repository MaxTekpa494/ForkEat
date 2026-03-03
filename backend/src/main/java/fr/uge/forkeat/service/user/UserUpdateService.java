package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.PasswordValidator;
import fr.uge.forkeat.service.exception.CheckProfileUpdateFailureException;
import fr.uge.forkeat.service.exception.RegisterFailureException;
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
    if (firstName.isBlank() || lastName.isBlank() || newUsername.isBlank()) {
      throw new CheckProfileUpdateFailureException("Veuillez remplir tous les champs");
    }

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
  public void requestEmailChange(String username, String newEmail, String currentPassword, String newPassword, String confirmPassword) {
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

    String pendingPasswordHash = null;
    if (user.authMode() == AuthMode.GOOGLE && newPassword != null) {
      if (!newPassword.equals(confirmPassword)) {
        throw new CheckProfileUpdateFailureException("Les mots de passe ne correspondent pas");
      }
      try {
        PasswordValidator.validate(newPassword);
      } catch (RegisterFailureException e) {
        throw new CheckProfileUpdateFailureException(e.getMessage());
      }
      pendingPasswordHash = passwordHasher.hash(newPassword);
    }

    if (pendingPasswordHash != null) {
      emailVerificationService.sendEmailChangeCode(user.id(), user.email(), newEmail, pendingPasswordHash);
    } else {
      emailVerificationService.sendEmailChangeCode(user.id(), user.email(), newEmail);
    }
  }

  @Transactional(
          isolation = Isolation.REPEATABLE_READ,
          timeout = 10
  )
  public void requestPasswordChange(String username, String currentPassword, String newPassword, String confirmPassword) {
    Objects.requireNonNull(username);
    Objects.requireNonNull(currentPassword);
    Objects.requireNonNull(newPassword);
    Objects.requireNonNull(confirmPassword);

    if (!newPassword.equals(confirmPassword)) {
      throw new CheckProfileUpdateFailureException("Les mots de passe ne correspondent pas");
    }

    try {
      PasswordValidator.validate(newPassword);
    } catch (RegisterFailureException e) {
      throw new CheckProfileUpdateFailureException(e.getMessage());
    }

    var user = userService.getUserByUsername(username);
    var storedHash = userPersistence.findPasswordHashByUsername(username);

    if (!passwordHasherPort.matches(currentPassword, storedHash)) {
      throw new CheckProfileUpdateFailureException("Incorrect current password");
    }

    if (passwordHasherPort.matches(newPassword, storedHash)) {
      throw new CheckProfileUpdateFailureException("Passwords are the same");
    }

    var pendingPasswordHash = passwordHasher.hash(newPassword);
    emailVerificationService.sendPasswordChangeCode(user.id(), user.email(), pendingPasswordHash);
  }

  @Transactional(
          isolation = Isolation.REPEATABLE_READ,
          timeout = 10
  )
  public void confirmForgotPasswordChange(String email, String code, String newPassword, String confirmPassword) {
    if (!newPassword.equals(confirmPassword)) {
      throw new CheckProfileUpdateFailureException("Les mots de passe ne correspondent pas");
    }
    try {
      PasswordValidator.validate(newPassword);
    } catch (RegisterFailureException e) {
      throw new CheckProfileUpdateFailureException(e.getMessage());
    }
    var user = userService.getUserByEmail(email);
    emailVerificationService.confirmPasswordReset(user.id(), code, passwordHasher.hash(newPassword));
  }

  @Transactional(
          isolation = Isolation.REPEATABLE_READ,
          timeout = 10
  )
  public void setPasswordForOAuthUser(String username, String newPassword, String confirmPassword) {
    Objects.requireNonNull(username);
    Objects.requireNonNull(newPassword);
    Objects.requireNonNull(confirmPassword);

    if (!newPassword.equals(confirmPassword)) {
      throw new CheckProfileUpdateFailureException("Les mots de passe ne correspondent pas");
    }

    try {
      PasswordValidator.validate(newPassword);
    } catch (RegisterFailureException e) {
      throw new CheckProfileUpdateFailureException(e.getMessage());
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
