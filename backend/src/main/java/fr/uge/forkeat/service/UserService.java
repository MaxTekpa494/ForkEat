package fr.uge.forkeat.service;


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

    @Transactional()
    public void likeRecipe(UUID userId, UUID recipeId){
        //if(!this.userPersistence.hasLikedRecipe(userId, recipeId)){
            this.userPersistence.likeRecipe(userId, recipeId);
        //}
    }
}
