package fr.uge.forkeat.service.port;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;

import java.util.Optional;
import java.util.UUID;

public interface UserIdentityPort {
    Optional<UUID> findIdByUsername(String username);

    default UUID findIdByUsernameOrThrow(String username) {
        return findIdByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
