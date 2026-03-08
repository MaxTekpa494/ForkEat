package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.UserProfileView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);
    Optional<UserProfileView> findProfileByUsername(String username);
    Optional<UserEntity> findById(UUID id);
    Optional<UserEntity> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Page<UserEntity> findByRole(UserRole role, Pageable pageable);
    long countByRole(UserRole role);

    @Query("SELECT u.id FROM UserEntity u WHERE u.username = :username")
    Optional<UUID> findIdByUsername(@Param("username") String username);
}