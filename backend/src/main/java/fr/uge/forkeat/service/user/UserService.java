package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.persistence.UserPersistence;
import fr.uge.forkeat.service.port.PasswordHasherPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserPersistence userPersistence;
    private final PasswordHasherPort passwordHasherPort;

    public UserService(UserPersistence userPersistence, PasswordHasherPort passwordHasherPort) {
        this.userPersistence = Objects.requireNonNull(userPersistence);
        this.passwordHasherPort = Objects.requireNonNull(passwordHasherPort);
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

}