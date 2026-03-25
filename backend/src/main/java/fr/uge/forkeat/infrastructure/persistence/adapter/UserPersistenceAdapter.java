package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.UserEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.neo4j.repository.Neo4jUserRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.projection.UserPublicProfile;
import fr.uge.forkeat.service.model.user.projection.UserSocialStats;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserPersistenceAdapter implements UserPersistence {

	private final UserRepository userRepository;

    private final Neo4jUserRepository neo4jUserRepository;

	public UserPersistenceAdapter(UserRepository userRepository, Neo4jUserRepository neo4jUserRepository) {
		this.userRepository = userRepository;
        this.neo4jUserRepository = neo4jUserRepository;
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
	public void updatePassword(UUID userId, String hashedPassword) {
		Objects.requireNonNull(userId);
		if (hashedPassword == null || hashedPassword.isEmpty()) {
			throw new IllegalArgumentException("Hashed password is null or empty");
		}
		var entity = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
		entity.setPassword(hashedPassword);
		userRepository.save(entity);
	}

	@Override
	public User updateUserAndPassword(User user, String hashedPassword) {
		Objects.requireNonNull(user);
		if (hashedPassword == null || hashedPassword.isEmpty()) {
			throw new IllegalArgumentException("Hashed password is null or empty");
		}
		var existing = loadAndApplyUserFields(user);
		existing.setPassword(hashedPassword);
		return UserEntityMapper.toDomain(userRepository.save(existing));
	}

	@Override
	public User updateUser(User user) {
		Objects.requireNonNull(user);
		var existing = loadAndApplyUserFields(user);
		return UserEntityMapper.toDomain(userRepository.save(existing));
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
	public boolean existsById(UUID userId) {
		return userRepository.existsById(userId);
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
	public PageResult<User> findAllByRole(UserRole role) {
		Objects.requireNonNull(role);
		var pageResult = userRepository.findByRole(role, Pageable.unpaged());
		var users = pageResult.getContent().stream().map(UserEntityMapper::toDomain).toList();
		return new PageResult<>(users, pageResult.getTotalElements());
	}

	@Override
	public PageResult<User> searchByRoleAndQuery(UserRole role, String query) {
		Objects.requireNonNull(role);
		Objects.requireNonNull(query);
		var pageResult = userRepository.searchByRoleAndQuery(role, query, Pageable.unpaged());
		var users = pageResult.getContent().stream().map(UserEntityMapper::toDomain).toList();
		return new PageResult<>(users, pageResult.getTotalElements());
	}
	
	@Override
	public long countByRole(UserRole role) {
		Objects.requireNonNull(role);
		return userRepository.countByRole(role);
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
	public Optional<UUID> findIdByUsername(String username) {
		Objects.requireNonNull(username);
		return userRepository.findIdByUsername(username);
	}

	@Override
	public boolean isFollowing(UUID followerId, UUID followedId) {
		Objects.requireNonNull(followerId);
		Objects.requireNonNull(followedId);
		return neo4jUserRepository.isFollowing(followerId, followedId);
	}

	@Override
	public void follow(UUID followerId, UUID followedId) {
		Objects.requireNonNull(followerId);
		Objects.requireNonNull(followedId);
		neo4jUserRepository.follow(followerId, followedId, Instant.now());
	}

	@Override
	public void unfollow(UUID followerId, UUID followedId) {
		Objects.requireNonNull(followerId);
		Objects.requireNonNull(followedId);
		neo4jUserRepository.unfollow(followerId, followedId);
	}

	@Override
	public UserPublicProfile findPublicProfile(UUID userId) {
		Objects.requireNonNull(userId);
		var user = userRepository.findProfileById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
		return new UserPublicProfile(
				user.getUsername(),
				user.getFirstName(),
				user.getLastName()
		);
	}

	@Override
	public UserSocialStats findUserSocialStats(UUID userId) {
		Objects.requireNonNull(userId);
		var counts = neo4jUserRepository.findSocialCountsByUserId(userId);
		return new UserSocialStats(
				counts != null ? counts.followerCount() : 0,
				counts != null ? counts.followingCount() : 0,
				counts != null ? counts.totalLikeCount() : 0,
				counts != null ? counts.totalSuperLikeCount() : 0
		);
	}

	@Override
	public List<String> findAllActiveMemberEmails() {
		return userRepository.findEmailsByStatusAndRole(
				fr.uge.forkeat.service.model.user.UserStatus.ACTIVE,
				UserRole.MEMBER
		);
	}

	@Override
	public void deleteById(UUID userId) {
		Objects.requireNonNull(userId);
		userRepository.deleteById(userId);
	}

	private UserEntity loadAndApplyUserFields(User user) {
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
		return existing;
	}

}

