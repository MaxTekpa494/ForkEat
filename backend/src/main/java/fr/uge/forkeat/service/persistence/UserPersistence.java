package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.projection.UserPublicProfile;
import fr.uge.forkeat.service.model.user.projection.UserSocialStats;
import fr.uge.forkeat.service.port.UserIdentityPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPersistence extends UserIdentityPort {
	User saveUser(User user, String hashedPassword);

	User updateUser(User user);

	void updatePassword(UUID userId, String hashedPassword);

	User updateUserAndPassword(User user, String hashedPassword);

	Optional<User> findById(UUID id);

	Optional<User> findByEmail(String email);

	Optional<User> findByUsername(String username);

	boolean existsById(UUID userId);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);

	String findPasswordHashByUsername(String username);

	void updateEmailVerified(UUID userId, boolean emailVerified);

	long countFollowers(UUID userId);

	long countFollowing(UUID userId);

	long countTotalLikesReceived(UUID userId);

	long countTotalSuperLikesReceived(UUID userId);

	UserPublicProfile findPublicProfile(UUID userId);

	UserSocialStats findUserSocialStats(UUID userId);

	Optional<UUID> findIdByUsername(String username);

	boolean isFollowing(UUID followerId, UUID followedId);

	void follow(UUID followerId, UUID followedId);

	void unfollow(UUID followerId, UUID followedId);

	PageResult<User> findAllByRole(UserRole role);

	long countByRole(UserRole role);

	/** Emails de tous les membres actifs avec email vérifié (pour les notifications de promotion). */
	List<String> findAllActiveMemberEmails();

	void deleteById(UUID userId);
}