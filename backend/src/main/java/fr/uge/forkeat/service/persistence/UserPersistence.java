package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserPersistence {
    User saveUser(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}