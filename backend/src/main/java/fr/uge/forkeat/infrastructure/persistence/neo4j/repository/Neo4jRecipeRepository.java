package fr.uge.forkeat.infrastructure.persistence.neo4j.repository;

import fr.uge.forkeat.infrastructure.persistence.neo4j.node.RecipeNode;
import fr.uge.forkeat.infrastructure.persistence.neo4j.node.UserNode;
import jakarta.annotation.Nonnull;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;
import java.util.UUID;

public interface Neo4jRecipeRepository extends Neo4jRepository<RecipeNode, UUID> {

    @Override
    @Nonnull
    Optional<RecipeNode> findById(UUID uuid);
}
