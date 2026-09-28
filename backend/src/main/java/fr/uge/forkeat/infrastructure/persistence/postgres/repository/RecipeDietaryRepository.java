package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeDietaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface RecipeDietaryRepository extends JpaRepository<RecipeDietaryEntity, UUID> {

  @Modifying
  @Query("DELETE FROM RecipeDietaryEntity di WHERE di.recipe.id = :recipeId")
  void deleteByRecipeId(@Param("recipeId") UUID recipeId);
}
