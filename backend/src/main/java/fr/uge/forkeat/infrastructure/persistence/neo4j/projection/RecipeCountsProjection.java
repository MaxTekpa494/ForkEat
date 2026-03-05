package fr.uge.forkeat.infrastructure.persistence.neo4j.projection;

public record RecipeCountsProjection(
        String recipeId,
        long likeCount,
        long superLikeCount,
        long followCount
) {
}