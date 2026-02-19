package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeIngredientEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredientEntity, UUID> {
    @Modifying
    //@Transactional
    @Query("DELETE FROM RecipeIngredientEntity ri WHERE ri.recipe.id = :recipeId")
    void deleteByRecipeId(@Param("recipeId") UUID recipeId);
}