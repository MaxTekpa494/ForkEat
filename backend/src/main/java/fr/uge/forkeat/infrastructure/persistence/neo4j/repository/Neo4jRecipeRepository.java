package fr.uge.forkeat.infrastructure.persistence.neo4j.repository;

import fr.uge.forkeat.infrastructure.persistence.neo4j.node.RecipeNode;
import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.RecipeCountsProjection;
import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.RecipeUserInteractionProjection;
import jakarta.annotation.Nonnull;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    @Query("""
            UNWIND $recipeIds AS rid
            OPTIONAL MATCH (:Recipe {id: rid})<-[l:LIKED]-()
            WITH rid, count(l) AS likeCount
            OPTIONAL MATCH (:Recipe {id: rid})<-[sl:SUPER_LIKED]-()
            RETURN rid AS recipeId, likeCount, count(sl) AS superLikeCount
            """)
    List<RecipeCountsProjection> findCountsByRecipeIds(@Param("recipeIds") List<String> recipeIds);

    @Query("""
            UNWIND $recipeIds AS rid
            OPTIONAL MATCH (u:User {username: $username})-[l:LIKED]->(:Recipe {id: rid})
            WITH rid, l IS NOT NULL AS likedByCurrentUser
            OPTIONAL MATCH (u2:User {username: $username})-[sl:SUPER_LIKED]->(:Recipe {id: rid})
            RETURN rid AS recipeId, likedByCurrentUser, sl IS NOT NULL AS superLikedByCurrentUser
            """)
    List<RecipeUserInteractionProjection> findUserInteractionsByRecipeIds(
            @Param("recipeIds") List<String> recipeIds,
            @Param("username") String username
    );

    @Query("""
            MATCH (u:User {username: $username})-[:PUBLISHED]->(r:Recipe)
            RETURN count(r)
            """)
    long countByAuthorUsername(@Param("username") String username);

    @Query("""
            MATCH (u:User {id: $userId})
            MATCH (r:Recipe {id: $recipeId})
            MERGE (u)-[l:LIKED]->(r)
            """)
    void likeRecipe(@Param("userId") UUID userId, @Param("recipeId") UUID recipeId);

    @Query("""
            MATCH (u:User {id: $userId})
            MATCH (r:Recipe {id: $recipeId})
            MATCH (u)-[l:LIKED]->(r)
            DELETE (l)
            """)
    void unlikeRecipe(@Param("userId") UUID userId, @Param("recipeId") UUID recipeId);
}
