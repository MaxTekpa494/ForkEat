package fr.uge.forkeat.service.user;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.User;
import fr.uge.forkeat.service.model.UserRole;
import fr.uge.forkeat.service.model.UserStatus;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class UserRegistrationService {

    private final UserPersistence userPersistence;
    private final WalletService walletService;
    private final PasswordEncoder passwordEncoder;

    UserRegistrationService(UserPersistence userPersistence, WalletService walletService, PasswordEncoder passwordEncoder){
        this.userPersistence = userPersistence;
        this.walletService = walletService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(
            isolation = Isolation.REPEATABLE_READ,
            timeout = 15
    )
    public User registerUser(String firstName, String lastName, String username,
                             String email, String password, UserRole role) throws ResourceNotFoundException {

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
                role,
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

    @Transactional(
            isolation = Isolation.REPEATABLE_READ,
            timeout = 15
    )
    public User registerUserFromOAuth2(String firstName, String lastName,
                                       String email, AuthMode authMode,
                                       String profilePicture) throws ResourceNotFoundException {

        // Vérifier que l'email n'existe pas déjà
        if (userPersistence.existsByEmail(email)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        var userId = UUID.randomUUID();

        // Générer un username unique depuis l'email
        var username = generateUsernameFromEmail(email);

        var user = new User(
                userId,
                username,
                firstName,
                lastName,
                email,
                null,  // PAS de password pour OAuth2
                Instant.now(),
                UserRole.MEMBER,
                UserStatus.ACTIVE,
                authMode,  // GOOGLE
                null
        );

        var savedUser = userPersistence.saveUser(user);

        // Créer le wallet
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
