package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.DietaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DietaryRepository extends JpaRepository<DietaryEntity, UUID> {
  List<DietaryEntity> findByNameIn(List<String> names);

}
