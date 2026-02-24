package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.neo4j.repository.Neo4jUserRepository;
import fr.uge.forkeat.infrastructure.persistence.mapper.UserEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.SuperLikeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.SuperLikeRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.WalletRepository;
import fr.uge.forkeat.service.exception.InsufficientFundsException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserPersistenceAdapter implements UserPersistence {

	private final UserRepository userRepository;

    private final Neo4jUserRepository neo4jUserRepository;

    private final SuperLikeRepository superLikeRepository;

    private final WalletRepository walletRepository;

	public UserPersistenceAdapter(UserRepository userRepository, Neo4jUserRepository neo4jUserRepository, SuperLikeRepository superLikeRepository, WalletRepository walletRepository) {
		this.userRepository = userRepository;
        this.neo4jUserRepository = neo4jUserRepository;
        this.superLikeRepository = superLikeRepository;
        this.walletRepository = walletRepository;
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
    public boolean hasLikedRecipe(UUID userId, UUID recipeId) {
        return this.neo4jUserRepository.hasLiked(userId, recipeId);

    }

    @Override
    public void likeRecipe(UUID userId, UUID recipeId) {
        neo4jUserRepository.likeRecipe(userId, recipeId);
    }

    @Override
    public void unlikeRecipe(UUID userId, UUID recipeId){
        neo4jUserRepository.unlikeRecipe(userId, recipeId);
    }

    @Override
    public void superLikeRecipe(UUID userId, UUID recipeId){
        var superLike = new SuperLikeEntity(userId, recipeId);
        var balance = walletRepository.findBalanceByUserId(userId);
        if(balance < 100){
            throw new InsufficientFundsException(balance, 100L);
        }
        walletRepository.decrementBalanceByUserId(userId, 100L);
        superLikeRepository.save(superLike);
    }

    @Override
    public boolean hasSuperLikedRecipe(UUID userId, UUID recipeId){
        return this.superLikeRepository.existsByRecipeIdAndUserId(Objects.requireNonNull(userId), Objects.requireNonNull(recipeId));
    }
}