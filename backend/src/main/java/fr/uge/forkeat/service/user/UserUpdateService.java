package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.User;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserUpdateService {
    private final UserPersistence userPersistence;
    private final UserQueryService userQueryService;
    private final PasswordEncoder passwordEncoder;

    UserUpdateService(UserPersistence userPersistence,
                      UserQueryService userQueryService,
                      PasswordEncoder passwordEncoder){
        this.userPersistence = userPersistence;
        this.userQueryService = userQueryService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(
            isolation = Isolation.REPEATABLE_READ,
            timeout = 10
    )
    public User updateProfile(UUID userId, String firstName, String lastName, String username) throws ResourceNotFoundException {
        var user = userQueryService.getUserById(userId);

        if (!user.username().equals(username) && userPersistence.existsByUsername(username)) {
            throw new IllegalArgumentException("This username is already used");
        }

        var updatedUser = new User(
                user.id(),
                username,
                firstName,
                lastName,
                user.email(),
                user.password(),
                user.createdAt(),
                user.role(),
                user.status(),
                user.authentificationMode(),
                user.walletId()
        );

        return userPersistence.saveUser(updatedUser);
    }

    @Transactional(
            isolation = Isolation.REPEATABLE_READ,
            timeout = 10
    )
    public User updateEmail(UUID userId, String newEmail, String currentPassword) throws ResourceNotFoundException {
        var user = userQueryService.getUserById(userId);

        // Vérifier le mot de passe actuel pour la sécurité
        if (!passwordEncoder.matches(currentPassword, user.password())) {
            throw new IllegalArgumentException("Mot de passe incorrect");
        }

        // Vérifier que le nouvel email n'est pas déjà utilisé
        if (!user.email().equals(newEmail) && userPersistence.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        var updatedUser = new User(
                user.id(),
                user.username(),
                user.firstName(),
                user.lastName(),
                newEmail,
                user.password(),
                user.createdAt(),
                user.role(),
                user.status(),
                user.authentificationMode(),
                user.walletId()
        );

        return userPersistence.saveUser(updatedUser);
    }

    @Transactional(
            isolation = Isolation.REPEATABLE_READ,
            timeout = 10
    )
    public void updatePassword(UUID userId, String currentPassword, String newPassword) throws ResourceNotFoundException {
        var user = userQueryService.getUserById(userId);

        if (!passwordEncoder.matches(currentPassword, user.password())) {
            throw new IllegalArgumentException("Mot de passe actuel incorrect");
        }

        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit contenir au moins 8 caractères");
        }

        var updatedUser = new User(
                user.id(),
                user.username(),
                user.firstName(),
                user.lastName(),
                user.email(),
                passwordEncoder.encode(newPassword),
                user.createdAt(),
                user.role(),
                user.status(),
                user.authentificationMode(),
                user.walletId()
        );

        userPersistence.saveUser(updatedUser);
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
                null,
                user.createdAt(),
                user.role(),
                user.status(),
                newAuthMode,
                user.walletId()
        );

        return userPersistence.saveUser(migratedUser);
    }
}
