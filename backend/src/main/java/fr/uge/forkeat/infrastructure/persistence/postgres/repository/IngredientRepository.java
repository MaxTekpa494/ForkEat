package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.IngredientEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IngredientRepository extends CrudRepository<IngredientEntity, UUID> {
  List<IngredientEntity> findByNameIngredient(List<String> names);
}
