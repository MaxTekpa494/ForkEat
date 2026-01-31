package fr.uge.forkeat.infrastructure.persistence.postgres.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.service.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.id());
        entity.setUsername(user.username());
        entity.setFirstName(user.firstName());
        entity.setLastName(user.lastName());
        entity.setEmail(user.email());
        entity.setPassword(user.password());
        entity.setCreatedAt(user.createdAt());
        entity.setRole(user.role());
        entity.setStatus(user.status());
        entity.setAuthMode(user.authentificationMode());
        return entity;
    }

    public User toModel(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        var walletId = (entity.getWallet() != null) ? entity.getWallet().getId() : null;

        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getCreatedAt(),
                entity.getRole(),
                entity.getStatus(),
                entity.getAuthMode(),
                walletId // On utilise la variable sécurisée
        );
    }
}
