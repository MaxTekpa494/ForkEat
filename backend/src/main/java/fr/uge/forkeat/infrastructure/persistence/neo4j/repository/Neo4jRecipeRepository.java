package fr.uge.forkeat.infrastructure.persistence.neo4j.repository;

import fr.uge.forkeat.infrastructure.persistence.neo4j.node.RecipeNode;
import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.RecipeCountsProjection;
import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.RecipeUserInteractionProjection;
import jakarta.annotation.Nonnull;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

import java.time.ZonedDateTime;
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
            OPTIONAL MATCH (u:User {id: $userId})-[l:LIKED]->(:Recipe {id: rid})
            WITH rid, l IS NOT NULL AS likedByCurrentUser
            OPTIONAL MATCH (u2:User {id: $userId})-[sl:SUPER_LIKED]->(:Recipe {id: rid})
            WITH rid, likedByCurrentUser, sl IS NOT NULL AS superLikedByCurrentUser
            OPTIONAL MATCH (u3:User {id: $userId})-[f:FOLLOWS_RECIPE]->(:Recipe {id: rid})
            RETURN rid AS recipeId, likedByCurrentUser, superLikedByCurrentUser, f IS NOT NULL AS followedByCurrentUser
            """)
    List<RecipeUserInteractionProjection> findUserInteractionsByRecipeIds(
            @Param("recipeIds") List<String> recipeIds,
            @Param("userId") String userId
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
            MATCH (me:User {username: $username})-[f:FEED]->(r:Recipe)
            WHERE f.createdAt >= $sinceTime
                  AND f.createdAt <= $beforeTime
            RETURN r
            ORDER BY f.depth ASC, r.createdAt DESC
            SKIP $offset LIMIT $limit
            """)
    List<RecipeNode> getFeed(@Param("username") String username, @Param("offset") long offset, @Param("limit") long limit, @Param("sinceTime") ZonedDateTime sinceTime, @Param("beforeTime") ZonedDateTime beforeTime);

    @Query("""
        MATCH (follower:User {username: $followerUsername})
        MATCH (followed:User {username: $followedUsername})
        MATCH (followed)-[:FOLLOWS*0..2]->(relay:User)-[:PUBLISHED]->(recipe:Recipe)
        WITH follower, recipe,
             min(length(shortestPath((followed)-[:FOLLOWS*0..2]->(relay))) + 1) AS depth
        MERGE (follower)-[f:FEED]->(recipe)
        ON CREATE SET f.depth = depth, f.createdAt = $now
        ON MATCH SET f.depth = CASE WHEN depth < f.depth THEN depth ELSE f.depth END
        WITH recipe
        MATCH path = (follower)<-[:FOLLOWS*1..2]-(upstream:User)
        WITH upstream, recipe, min(length(path)) + 1 AS upstreamDepth
        MERGE (upstream)-[f:FEED]->(recipe)
        ON CREATE SET f.depth = upstreamDepth, f.createdAt = $now
        ON MATCH SET f.depth = CASE WHEN upstreamDepth < f.depth THEN upstreamDepth ELSE f.depth END
        """)
    void propagateFeedOnFollow(
            @Param("followerUsername") String followerUsername,
            @Param("followedUsername") String followedUsername,
            @Param("now") ZonedDateTime now
    );


    @Query("MATCH (:User {username: $username})-[r:FEED]->() " +
            "WHERE r.createdAt >= $date " +
            "RETURN count(r)")
    long countFeedRelationshipsBefore(
            @Param("username") String username,
            @Param("date") ZonedDateTime date
    );


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
