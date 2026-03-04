package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.persistence.UserPersistence;
import fr.uge.forkeat.service.port.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserPersistence userPersistence;
    private final PasswordHasher passwordHasher;

    public UserService(UserPersistence userPersistence, PasswordHasher passwordHasher) {
        this.userPersistence = Objects.requireNonNull(userPersistence);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
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
        return passwordHasher.matches(rawPassword, hashedPassword);
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

}