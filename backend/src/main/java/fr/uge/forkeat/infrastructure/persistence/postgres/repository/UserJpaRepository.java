package fr.uge.forkeat.infrastructure.persistence.postgres.repository;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface UserJpaRepository extends CrudRepository<UserEntity, Long> {}