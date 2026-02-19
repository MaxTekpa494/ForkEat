package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeAllergenEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RecipeAllergenRepository extends JpaRepository<RecipeAllergenEntity, UUID> {

    // @Query force un DELETE SQL direct (1 requête bulk).
    // Sans @Query, Spring Data fait SELECT puis DELETE un par un → risque de conflit avec les INSERT
    // Au momment du flush, on a des problème de contrainte d'unicité car quand t-on ne fait le
    // Query, spring planifie la suppression mais le problème c'est qu'au moment du flush
    // le select et le delete rentre en conflit...
    @Modifying
    @Query("DELETE FROM RecipeAllergenEntity ra WHERE ra.recipe.id = :recipeId")
    void deleteByRecipeId(@Param("recipeId") UUID recipeId);
}