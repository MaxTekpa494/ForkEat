package fr.uge.forkeat.service;

import fr.uge.forkeat.service.model.User;
import fr.uge.forkeat.service.model.UserRole;
import fr.uge.forkeat.service.model.UserStatus;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.persistence.UserPersistence;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class UserService {

    private final UserPersistence userPersistence;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;

    public UserService(UserPersistence userPersistence,
                       PasswordEncoder passwordEncoder,
                       WalletService walletService) {
        this.userPersistence = userPersistence;
        this.passwordEncoder = passwordEncoder;
        this.walletService = walletService;
    }

    @Transactional(
            isolation = Isolation.REPEATABLE_READ,
            timeout = 15
    )
    public User registerUser(String firstName, String lastName, String username,
                             String email, String password) throws ResourceNotFoundException {

        if (userPersistence.existsByEmail(email)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        // Vérifier si le username existe déjà
        if (userPersistence.existsByUsername(username)) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà pris");
        }

        var userId = UUID.randomUUID();

        var user = new User(
                userId,
                username,
                firstName,
                lastName,
                email,
                passwordEncoder.encode(password),
                Instant.now(),
                UserRole.MEMBER,
                UserStatus.ACTIVE,
                AuthMode.LOCAL,
                null
        );

        var savedUser = userPersistence.saveUser(user);

        var wallet = walletService.createWallet(savedUser.id());

        var userWithWallet = new User(
                savedUser.id(),
                savedUser.username(),
                savedUser.firstName(),
                savedUser.lastName(),
                savedUser.email(),
                savedUser.password(),
                savedUser.createdAt(),
                savedUser.role(),
                savedUser.status(),
                savedUser.authentificationMode(),
                wallet.id()
        );

        return userPersistence.saveUser(userWithWallet);
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) throws ResourceNotFoundException {
        return userPersistence.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID id) throws ResourceNotFoundException {
        return userPersistence.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) throws ResourceNotFoundException {
        return userPersistence.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé : " + username));
    }

    @Transactional(
            isolation = Isolation.REPEATABLE_READ,
            timeout = 10
    )
    public User updateProfile(UUID userId, String firstName, String lastName, String username) throws ResourceNotFoundException {
        var user = getUserById(userId);

        if (!user.username().equals(username) && userPersistence.existsByUsername(username)) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà pris");
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
        var user = getUserById(userId);

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
        var user = getUserById(userId);

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
}