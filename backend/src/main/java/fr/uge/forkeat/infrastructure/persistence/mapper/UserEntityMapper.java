package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.service.model.user.User;

import java.util.Objects;

public final class UserEntityMapper {

    private UserEntityMapper() {}

    public  static User toDomain(UserEntity entity) {
        Objects.requireNonNull(entity);
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getRole(),
                entity.getStatus(),
                entity.getAuthMode(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.isEmailVerified()
        );
    }

    public static UserEntity toEntity(User user) {
        Objects.requireNonNull(user);
        var entity = new UserEntity();
        entity.setId(user.id());
        entity.setUsername(user.username());
        entity.setFirstName(user.firstName());
        entity.setLastName(user.lastName());
        entity.setEmail(user.email());
        entity.setRole(user.role());
        entity.setStatus(user.status());
        entity.setAuthMode(user.authMode());
        entity.setCreatedAt(user.createdAt());
        entity.setUpdatedAt(user.updatedAt());
        entity.setEmailVerified(user.emailVerified());
        return entity;
    }
}