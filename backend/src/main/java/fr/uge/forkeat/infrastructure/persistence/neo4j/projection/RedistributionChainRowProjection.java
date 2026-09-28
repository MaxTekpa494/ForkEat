package fr.uge.forkeat.infrastructure.persistence.neo4j.projection;

public record RedistributionChainRowProjection(
        String authorId,
        String username,
        String recipeId,
        String recipeTitle,
        Long amountCents
) {}