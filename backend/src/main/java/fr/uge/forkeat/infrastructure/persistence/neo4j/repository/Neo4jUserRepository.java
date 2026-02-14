package fr.uge.forkeat.infrastructure.persistence.neo4j.repository;

import fr.uge.forkeat.infrastructure.persistence.neo4j.node.UserNode;
import jakarta.validation.constraints.NotNull;
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
        MATCH (p1:User {uuid: $userId})
        MATCH (p2:Recipe {uuid: $recipeId})
        RETURN EXISTS((p1)-[:LIKED]->(p2))
        """)
    boolean hasLiked(
            @Param("from") UUID userId,
            @Param("to") UUID recipeId
    );

    @NonNull
    @Override
    Optional<UserNode> findById(UUID id);


}
