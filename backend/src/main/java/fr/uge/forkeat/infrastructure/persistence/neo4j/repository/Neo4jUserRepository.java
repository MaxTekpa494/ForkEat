package fr.uge.forkeat.infrastructure.persistence.neo4j.repository;

import fr.uge.forkeat.infrastructure.persistence.neo4j.node.UserNode;
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

}
