package fr.uge.forkeat.infrastructure.persistence.neo4j.projection;

public record EarningsRowProjection(
        String batchMonth,
        Long amountCents,
        String sourceRecipeId,
        String recipeId,
        String recipeTitle
) {}