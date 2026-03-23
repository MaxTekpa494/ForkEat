package fr.uge.forkeat.infrastructure.persistence.neo4j.repository;

import fr.uge.forkeat.infrastructure.persistence.neo4j.node.RecipeNode;
import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.RecipeCountsProjection;
import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.RecipeUserInteractionProjection;
import jakarta.annotation.Nonnull;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

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
            WITH rid, likeCount, count(sl) AS superLikeCount
            OPTIONAL MATCH (:Recipe {id: rid})<-[f:FOLLOWS_RECIPE]-()
            RETURN rid AS recipeId, likeCount, superLikeCount, count(f) AS followCount
            """)
    List<RecipeCountsProjection> findCountsByRecipeIds(@Param("recipeIds") List<String> recipeIds);

    @Query("""
            UNWIND $recipeIds AS rid
            OPTIONAL MATCH (u:User {username: $username})-[l:LIKED]->(:Recipe {id: rid})
            WITH rid, l IS NOT NULL AS likedByCurrentUser
            OPTIONAL MATCH (u2:User {username: $username})-[sl:SUPER_LIKED]->(:Recipe {id: rid})
            WITH rid, likedByCurrentUser, sl IS NOT NULL AS superLikedByCurrentUser
            OPTIONAL MATCH (u3:User {username: $username})-[f:FOLLOWS_RECIPE]->(:Recipe {id: rid})
            RETURN rid AS recipeId, likedByCurrentUser, superLikedByCurrentUser, f IS NOT NULL AS followedByCurrentUser
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

    @Query("""
            MATCH (u:User {id: $userId})
            MATCH (r:Recipe {id: $recipeId})
            MERGE (u)-[f:FOLLOWS_RECIPE]->(r)
            ON CREATE SET f.since = $since
            """)
    void followRecipe(@Param("userId") UUID userId,
                      @Param("recipeId") UUID recipeId,
                      @Param("since") Instant since);

    @Query("""
            MATCH (u:User {id: $userId})-[f:FOLLOWS_RECIPE]->(r:Recipe {id: $recipeId})
            DELETE f
            """)
    void unfollowRecipe(@Param("userId") UUID userId, @Param("recipeId") UUID recipeId);

    @Query("""
            MATCH (r:Recipe)
            WHERE r.deletedAt IS NULL
            OPTIONAL MATCH (r)<-[l:LIKED]-()
            OPTIONAL MATCH (r)<-[sl:SUPER_LIKED]-()
            WITH r.id AS recipeId, count(DISTINCT l) + count(DISTINCT sl) AS score
            ORDER BY score DESC
            LIMIT $limit
            RETURN recipeId
            """)
    List<String> findTopLikedRecipeIds(@Param("limit") int limit);
}
