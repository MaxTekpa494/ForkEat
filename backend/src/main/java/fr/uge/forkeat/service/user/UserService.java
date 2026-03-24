package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.WalletNotFoundException;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;
import fr.uge.forkeat.service.persistence.PlatformWalletPersistence;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import fr.uge.forkeat.service.persistence.UserPersistence;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import fr.uge.forkeat.service.port.PasswordHasherPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserPersistence userPersistence;
    private final PasswordHasherPort passwordHasherPort;
    private final WalletPersistence walletPersistence;
    private final RecipePersistence recipePersistence;
    private final PlatformWalletPersistence platformWalletPersistence;

    public UserService(UserPersistence userPersistence, PasswordHasherPort passwordHasherPort,
                       WalletPersistence walletPersistence, RecipePersistence recipePersistence,
                       PlatformWalletPersistence platformWalletPersistence) {
        this.userPersistence = Objects.requireNonNull(userPersistence);
        this.passwordHasherPort = Objects.requireNonNull(passwordHasherPort);
        this.walletPersistence = Objects.requireNonNull(walletPersistence);
        this.recipePersistence = Objects.requireNonNull(recipePersistence);
        this.platformWalletPersistence = Objects.requireNonNull(platformWalletPersistence);
    }

    public User getUserByEmail(String email) {
        return userPersistence.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public Optional<User> findByEmail(String email) {
        return userPersistence.findByEmail(email);
    }

    public User getUserById(UUID id) {
        return userPersistence.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public User getUserByUsername(String username) {
        return userPersistence.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Transactional(readOnly = true)
    public PageResult<User> getUsersByRole(UserRole role) {
        Objects.requireNonNull(role);
        return userPersistence.findAllByRole(role);
    }

    @Transactional(readOnly = true)
    public long countByRole(UserRole role) {
        Objects.requireNonNull(role);
        return userPersistence.countByRole(role);
    }

    public boolean checkUserPassword(String username, String rawPassword) {
        var hashedPassword = userPersistence.findPasswordHashByUsername(username);
        return passwordHasherPort.matches(rawPassword, hashedPassword);
    }

    @Transactional
    public void follow(String followerUsername, String followedUsername) {
        Objects.requireNonNull(followerUsername);
        Objects.requireNonNull(followedUsername);
        var followerId = userPersistence.findIdByUsernameOrThrow(followerUsername);
        var followedId = userPersistence.findIdByUsernameOrThrow(followedUsername);
        userPersistence.follow(followerId, followedId);
    }

    @Transactional
    public void unfollow(String followerUsername, String followedUsername) {
        Objects.requireNonNull(followerUsername);
        Objects.requireNonNull(followedUsername);
        var followerId = userPersistence.findIdByUsernameOrThrow(followerUsername);
        var followedId = userPersistence.findIdByUsernameOrThrow(followedUsername);
        userPersistence.unfollow(followerId, followedId);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 30)
    public void deleteAccount(String username) {
        Objects.requireNonNull(username);
        var user = userPersistence.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        // Verrou pessimiste sur le wallet de l'utilisateur
        var userWallet = walletPersistence.loadWalletWithLock(user.id()).orElseThrow(() -> new WalletNotFoundException(user.id()));
        // Récupération du wallet earnings (le transfert via incrementBalanceById est atomique)
        var earningsWallet = walletPersistence.getEarningsWallet();
        // Transfert du solde vers le wallet des gains + trace dans l'historique plateforme
        if (userWallet.balance() > 0) {
            walletPersistence.incrementBalanceById(earningsWallet.id(), userWallet.balance());
            platformWalletPersistence.recordTransaction(
                    PlatformWalletType.EARNINGS,
                    userWallet.balance(),
                    "ACCOUNT_DELETION",
                    user.id()   // referenceId = UUID de l'utilisateur supprimé (traçabilité)
            );
        }
        // Réassignation de toutes les recettes vers l'utilisateur système earnings
        recipePersistence.reassignRecipesToUser(user.id(), earningsWallet.userId());
        // Suppression de l'utilisateur (cascade JPA : wallet + bankInfo supprimés automatiquement)
        userPersistence.deleteById(user.id());
    }

}