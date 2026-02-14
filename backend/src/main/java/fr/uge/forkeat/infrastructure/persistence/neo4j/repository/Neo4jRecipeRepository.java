package fr.uge.forkeat.infrastructure.persistence.neo4j.repository;

import fr.uge.forkeat.infrastructure.persistence.neo4j.node.RecipeNode;
import fr.uge.forkeat.infrastructure.persistence.neo4j.node.UserNode;
import jakarta.annotation.Nonnull;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface Neo4jRecipeRepository extends Neo4jRepository<RecipeNode, UUID> {

    @Override
    @Nonnull
    Optional<RecipeNode> findById(UUID uuid);

    @Query("""
            MATCH (r:Recipe {id: $recipeId})
            OPTIONAL MATCH (r)<-[:LIKED]-()
            RETURN count(*) AS nbLike
            """)
    long nbLike(@Param("recipeId") UUID recipeId);
}
