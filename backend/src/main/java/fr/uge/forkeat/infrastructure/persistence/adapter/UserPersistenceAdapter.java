package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.UserEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.neo4j.repository.Neo4jUserRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.*;
import fr.uge.forkeat.service.model.user.projection.UserProfile;
import fr.uge.forkeat.service.model.user.projection.UserPublicProfile;
import fr.uge.forkeat.service.model.user.projection.UserSocialStats;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserPersistenceAdapter implements UserPersistence {

	private final UserRepository userRepository;
	private final Neo4jUserRepository neo4jUserRepository;

	public UserPersistenceAdapter(UserRepository userRepository, Neo4jUserRepository neo4jUserRepository) {
		this.userRepository = Objects.requireNonNull(userRepository);
		this.neo4jUserRepository = Objects.requireNonNull(neo4jUserRepository);
	}

	@Override
	public User saveUser(User user, String hashedPassword) {
		Objects.requireNonNull(user);
		var entity = UserEntityMapper.toEntity(user);

		if (Objects.requireNonNull(user.authMode()) == AuthMode.LOCAL) {
		  if (hashedPassword == null || hashedPassword.isEmpty()) {
			throw new IllegalArgumentException("Hashed password is null or empty");
		  }
		  entity.setPassword(hashedPassword);
		}

		var saved = userRepository.save(entity);
		return UserEntityMapper.toDomain(saved);
	}


	@Override
	public User updateUser(User user) {
		Objects.requireNonNull(user);
		var existing = userRepository.findById(user.id())
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + user.id()));
		existing.setUsername(user.username());
		existing.setFirstName(user.firstName());
		existing.setLastName(user.lastName());
		existing.setEmail(user.email());
		existing.setRole(user.role());
		existing.setStatus(user.status());
		existing.setAuthMode(user.authMode());
		existing.setUpdatedAt(user.updatedAt());
		var userUpdated = userRepository.save(existing);
		return UserEntityMapper.toDomain(userUpdated);
	}

	@Override
	public String findPasswordHashByUsername(String username) {
		Objects.requireNonNull(username);
		var user = userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
		return user.getPassword();
	}

	@Override
	public Optional<User> findById(UUID id) {
		return userRepository.findById(id).map(UserEntityMapper::toDomain);
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return userRepository.findByEmail(email).map(UserEntityMapper::toDomain);
	}

	@Override
	public Optional<User> findByUsername(String username) {
		return userRepository.findByUsername(username).map(UserEntityMapper::toDomain);
	}

	@Override
	public boolean existsByEmail(String email) {
		return userRepository.existsByEmail(email);
	}

	@Override
	public boolean existsByUsername(String username) {
		return userRepository.existsByUsername(username);
	}

	@Override
	public void updateEmailVerified(UUID userId, boolean emailVerified) {
		var entity = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
		entity.setEmailVerified(emailVerified);
		userRepository.save(entity);
	}

	@Override
	public long countFollowers(UUID userId) {
		Objects.requireNonNull(userId);
		return neo4jUserRepository.countFollowers(userId);
	}

	@Override
	public long countFollowing(UUID userId) {
		Objects.requireNonNull(userId);
		return neo4jUserRepository.countFollowing(userId);
	}

	@Override
	public long countTotalLikesReceived(UUID userId) {
		Objects.requireNonNull(userId);
		return neo4jUserRepository.countTotalLikesReceived(userId);
	}

	@Override
	public long countTotalSuperLikesReceived(UUID userId) {
		Objects.requireNonNull(userId);
		return neo4jUserRepository.countTotalSuperLikesReceived(userId);
	}

	@Override
	public boolean isFollowing(String followerUsername, String followedUsername) {
		Objects.requireNonNull(followerUsername);
		Objects.requireNonNull(followedUsername);
		return neo4jUserRepository.isFollowing(followerUsername, followedUsername);
	}

	@Override
	public UserProfile findUserProfile(String username) {
		Objects.requireNonNull(username);
		var user = userRepository.findProfileByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
		var counts = neo4jUserRepository.findSocialCountsByUserId(user.getId());
		var publicProfile = new UserPublicProfile(
				user.getUsername(),
				user.getFirstName(),
				user.getLastName()
		);
		var socialStats = new UserSocialStats(
				counts != null ? counts.followerCount() : 0,
				counts != null ? counts.followingCount() : 0,
				counts != null ? counts.totalLikeCount() : 0,
				counts != null ? counts.totalSuperLikeCount() : 0
		);
		return new UserProfile(publicProfile, socialStats);
	}
}