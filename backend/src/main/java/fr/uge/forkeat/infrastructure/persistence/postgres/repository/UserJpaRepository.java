package fr.uge.forkeat.infrastructure.persistence.postgres.repository;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {}