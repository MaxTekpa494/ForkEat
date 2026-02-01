package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.AllergenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AllergenRepository extends CrudRepository<AllergenEntity, UUID> {

    Optional<AllergenEntity> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
