package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeRagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipeRagRepository extends JpaRepository<RecipeRagEntity, UUID> {

  Optional<RecipeRagEntity> findByRecipe_Id(UUID recipeId);

  boolean existsByRecipe_Id(UUID recipeId);


  // C'est pour l'ApplicationRunner
  @Query(value = """
            SELECT r.id FROM recipes r
            WHERE r.status = 'PUBLISHED'
            AND NOT EXISTS (
                SELECT 1 FROM recipe_rag rag
                WHERE rag.recipe_id = r.id
            )
            """, nativeQuery = true)
  List<UUID> findRecipeIdsWithoutRagEntry();


  @Modifying
  @Query(value = """
            UPDATE recipe_rag
            SET embedding = CAST(:embedding AS vector),
                updated_at = now()
            WHERE recipe_id = :recipeId
            """, nativeQuery = true)
  void updateEmbedding(@Param("recipeId") UUID recipeId, @Param("embedding") String embedding);


  @Query(value = """
            SELECT recipe_id FROM recipe_rag
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
            LIMIT :topK
            """, nativeQuery = true)
  List<UUID> findTopKSimilar(@Param("queryEmbedding") String queryEmbedding, @Param("topK") int topK);



}
