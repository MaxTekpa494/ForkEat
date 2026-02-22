package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.projection.UserProfile;

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

	long countFollowers(UUID userId);

	long countFollowing(UUID userId);

	long countTotalLikesReceived(UUID userId);

	long countTotalSuperLikesReceived(UUID userId);

	UserProfile findUserProfile(String username);

	boolean isFollowing(String followerUsername, String followedUsername);
}