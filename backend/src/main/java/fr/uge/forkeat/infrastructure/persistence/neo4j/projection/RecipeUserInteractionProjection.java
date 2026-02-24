package fr.uge.forkeat.infrastructure.persistence.neo4j.projection;

public record RecipeUserInteractionProjection(
        String recipeId,
        boolean likedByCurrentUser,
        boolean superLikedByCurrentUser
) {
}