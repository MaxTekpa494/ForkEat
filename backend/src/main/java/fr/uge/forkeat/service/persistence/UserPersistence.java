package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPersistence {
	User saveUser(User user, String hashedPassword);

	User updateUser(User user);

	Optional<User> findById(UUID id);

	Optional<User> findByEmail(String email);

	Optional<User> findByUsername(String username);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);

	String findPasswordHashByUsername(String username);

	void updateEmailVerified(UUID userId, boolean emailVerified);

	List<User> findAllByRole(UserRole role);

	long countByRole(UserRole role);
}