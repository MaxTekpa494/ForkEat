package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserQueryService {
    private final UserPersistence userPersistence;

    UserQueryService(UserPersistence userPersistence) {
        this.userPersistence = Objects.requireNonNull(userPersistence);
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userPersistence.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userPersistence.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userPersistence.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userPersistence.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
}
