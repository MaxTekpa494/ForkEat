package fr.uge.forkeat.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.forkeat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
