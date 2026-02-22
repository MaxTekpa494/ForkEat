package fr.uge.forkeat.infrastructure.persistence.neo4j.repository;

import fr.uge.forkeat.infrastructure.persistence.neo4j.node.UserNode;
import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.UserSocialCountsProjection;
import org.jspecify.annotations.NonNull;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface Neo4jUserRepository extends Neo4jRepository<UserNode, UUID> {

    @Query("""
        MATCH (p1:User {id: $userId})
        MATCH (p2:Recipe {id: $recipeId})
        RETURN EXISTS((p1)-[:LIKED]->(p2)) As result
        """)
    boolean hasLiked(
            @Param("userId") UUID userId,
            @Param("recipeId") UUID recipeId
    );

    @NonNull
    @Override
    Optional<UserNode> findById(UUID id);

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
            OPTIONAL MATCH (u)<-[f:FOLLOWS]-()
            WITH u, count(f) AS followerCount
            OPTIONAL MATCH (u)-[fg:FOLLOWS]->()
            WITH u, followerCount, count(fg) AS followingCount
            OPTIONAL MATCH (u)-[:PUBLISHED]->(:Recipe)<-[l:LIKED]-()
            WITH u, followerCount, followingCount, count(l) AS totalLikeCount
            OPTIONAL MATCH (u)-[:PUBLISHED]->(:Recipe)<-[sl:SUPER_LIKED]-()
            RETURN followerCount, followingCount, totalLikeCount, count(sl) AS totalSuperLikeCount
            """)
    UserSocialCountsProjection findSocialCountsByUserId(@Param("userId") UUID userId);

    @Query("OPTIONAL MATCH (:User)-[f:FOLLOWS]->(:User {id: $userId}) RETURN count(f)")
    long countFollowers(@Param("userId") UUID userId);

    @Query("OPTIONAL MATCH (:User {id: $userId})-[f:FOLLOWS]->(:User) RETURN count(f)")
    long countFollowing(@Param("userId") UUID userId);

    @Query("OPTIONAL MATCH (:User {id: $userId})-[:PUBLISHED]->(:Recipe)<-[l:LIKED]-() RETURN count(l)")
    long countTotalLikesReceived(@Param("userId") UUID userId);

    @Query("OPTIONAL MATCH (:User {id: $userId})-[:PUBLISHED]->(:Recipe)<-[sl:SUPER_LIKED]-() RETURN count(sl)")
    long countTotalSuperLikesReceived(@Param("userId") UUID userId);

    @Query("""
            RETURN EXISTS((:User {username: $followerUsername})-[:FOLLOWS]->(:User {username: $followedUsername}))
            """)
    boolean isFollowing(@Param("followerUsername") String followerUsername,
                        @Param("followedUsername") String followedUsername);
}
