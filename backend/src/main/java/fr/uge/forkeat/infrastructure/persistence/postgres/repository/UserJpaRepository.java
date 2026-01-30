package fr.uge.forkeat.infrastructure.persistence.postgres.repository;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends CrudRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findById(UUID id);
    Boolean existsByEmail(String email);
    Boolean existsByUsername(String userName);
}