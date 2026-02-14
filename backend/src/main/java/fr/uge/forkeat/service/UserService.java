package fr.uge.forkeat.service;


import fr.uge.forkeat.service.exception.AlreadyLikedException;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class UserService {
    private final UserPersistence userPersistence;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserPersistence userPersistence, PasswordEncoder passwordEncoder){
        this.userPersistence = Objects.requireNonNull(userPersistence);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
    }

    public boolean checkUserPassword(String username, String rawPassword){
        var hashedPassword = userPersistence.findPasswordHashByUsername(username);
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }

    @Transactional("neo4jTransactionManager")
    public void likeRecipe(UUID userId, UUID recipeId){
        this.userPersistence.likeRecipe(Objects.requireNonNull(userId), Objects.requireNonNull(recipeId));
    }

    @Transactional("neo4jTransactionManager")
    public void unlikeRecipe(UUID userId, UUID recipeId){
        this.userPersistence.unlikeRecipe(Objects.requireNonNull(userId), Objects.requireNonNull(recipeId));
    }

    @Transactional("neo4jTransactionManager")
    public boolean hasLikedRecipe(UUID userId, UUID recipeId){
        return this.userPersistence.hasLikedRecipe(Objects.requireNonNull(userId), Objects.requireNonNull(recipeId));
    }
}
