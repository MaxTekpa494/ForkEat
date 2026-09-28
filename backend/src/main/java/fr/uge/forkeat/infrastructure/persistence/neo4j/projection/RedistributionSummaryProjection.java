package fr.uge.forkeat.infrastructure.persistence.neo4j.projection;

public record RedistributionSummaryProjection(
        String batchMonth,
        String sourceRecipeId,
        String sourceRecipeTitle,
        Long totalCents
) {}