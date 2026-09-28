package fr.uge.forkeat.infrastructure.persistence.neo4j.projection;

import java.time.Instant;

public record UnprocessedSLProjection(
        String superLikeId,
        String recipeId,
        Long redistAmount,
        Instant date
) {
}