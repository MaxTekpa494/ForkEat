package fr.uge.forkeat.infrastructure.persistence.neo4j.projection;

public record ChainNodeProjection(
        String authorId,
        String recipeId,
        Integer depth
) {
}