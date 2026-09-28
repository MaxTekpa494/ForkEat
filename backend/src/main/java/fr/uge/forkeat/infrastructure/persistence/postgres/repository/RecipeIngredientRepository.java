package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeIngredientEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredientEntity, UUID> {
    @Modifying
    @Query("DELETE FROM RecipeIngredientEntity ri WHERE ri.recipe.id = :recipeId")
    void deleteByRecipeId(@Param("recipeId") UUID recipeId);

    @Query("SELECT DISTINCT ri.unit FROM RecipeIngredientEntity ri WHERE ri.unit IS NOT NULL AND ri.unit <> '' ORDER BY ri.unit")
    List<String> findAllDistinctUnits();
}