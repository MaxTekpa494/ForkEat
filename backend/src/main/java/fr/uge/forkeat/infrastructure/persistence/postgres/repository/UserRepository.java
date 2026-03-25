package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.UserProfileView;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);
    Optional<UserProfileView> findProfileByUsername(String username);
    Optional<UserProfileView> findProfileById(UUID id);
    Optional<UserEntity> findById(UUID id);
    Optional<UserEntity> findByEmail(String email);
    boolean existsById(UUID userId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Page<UserEntity> findByRole(UserRole role, Pageable pageable);
    long countByRole(UserRole role);

    @Query("SELECT u.id FROM UserEntity u WHERE u.username = :username")
    Optional<UUID> findIdByUsername(@org.springframework.data.repository.query.Param("username") String username);

    /** Emails de tous les membres actifs avec email vérifié (pour les notifications de promotion). */
    @Query("SELECT u.email FROM UserEntity u WHERE u.status = :status AND u.emailVerified = true AND u.role = :role")
    List<String> findEmailsByStatusAndRole(@org.springframework.data.repository.query.Param("status") UserStatus status,
                                           @org.springframework.data.repository.query.Param("role") UserRole role);
}
