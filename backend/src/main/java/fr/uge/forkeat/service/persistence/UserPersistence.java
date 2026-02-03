package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.user.User;

import java.util.Optional;
import java.util.UUID;

public interface UserPersistence {
	User saveUser(User user, String hashedPassword);

	User updateWallet(User user, String walletId);

	Optional<User> findById(UUID id);

	Optional<User> findByEmail(String email);

	Optional<User> findByUsername(String username);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);
}