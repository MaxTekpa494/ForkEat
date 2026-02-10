package fr.uge.forkeat.service.user;

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

@Service
public class UserUpdateService {
  private final UserPersistence userPersistence;
  private final UserQueryService userQueryService;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationService emailVerificationService;

  UserUpdateService(UserPersistence userPersistence,
                    UserQueryService userQueryService,
                    PasswordEncoder passwordEncoder,
                    EmailVerificationService emailVerificationService) {
    this.userPersistence = Objects.requireNonNull(userPersistence);
    this.userQueryService = Objects.requireNonNull(userQueryService);
    this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
    this.emailVerificationService = Objects.requireNonNull(emailVerificationService);
  }

  @Transactional(
          isolation = Isolation.REPEATABLE_READ,
          timeout = 10
  )
  public User updateProfile(String currentUsername, String newUsername, String firstName, String lastName) {
    var user = userQueryService.getUserByUsername(currentUsername);

    if (!user.username().equals(newUsername)) {
      if (userPersistence.existsByUsername(newUsername)) {
        throw new CheckProfileUpdateFailure("Ce nom d'utilisateur est déjà pris");
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
    var user = userQueryService.getUserByUsername(username);

    if (user.authMode() == AuthMode.LOCAL) {
      var storedHash = userPersistence.findPasswordHashByUsername(username);
      if (!passwordEncoder.matches(currentPassword, storedHash)) {
        throw new CheckProfileUpdateFailure("Incorrect password");
      }
    }

    if (!user.email().equals(newEmail) && userPersistence.existsByEmail(newEmail)) {
      throw new CheckProfileUpdateFailure("Cet email est déjà utilisé");
    }

    emailVerificationService.sendEmailChangeCode(user.id(), user.email(), newEmail);
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
      throw new CheckProfileUpdateFailure("New password must be at least 8 characters");
    }

    var user = userQueryService.getUserByUsername(username);
    var storedHash = userPersistence.findPasswordHashByUsername(username);

    if (!passwordEncoder.matches(currentPassword, storedHash)) {
      throw new CheckProfileUpdateFailure("Incorrect current password");
    }

    if (passwordEncoder.matches(newPassword, storedHash)) {
      throw new CheckProfileUpdateFailure("Passwords are the same");
    }

    var hashedNewPassword = passwordEncoder.encode(newPassword);
    emailVerificationService.sendPasswordChangeCode(user.id(), user.email(), hashedNewPassword);
  }

  @Transactional(
          isolation = Isolation.REPEATABLE_READ,
          timeout = 10
  )
  public void setPasswordForOAuthUser(String username, String newPassword) {
    Objects.requireNonNull(username);
    Objects.requireNonNull(newPassword);

    if (newPassword.length() < 8) {
      throw new CheckProfileUpdateFailure("New password must be at least 8 characters");
    }

    var user = userQueryService.getUserByUsername(username);

    if (user.authMode() != AuthMode.GOOGLE) {
      throw new CheckProfileUpdateFailure("This user already has a local password");
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

    userPersistence.saveUser(updatedUser, passwordEncoder.encode(newPassword));
  }
}
