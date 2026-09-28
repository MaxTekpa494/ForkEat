package fr.uge.forkeat.service.model.recipe;

public record RecipeUserInteraction(
        boolean likedByCurrentUser,
        boolean superLikedByCurrentUser,
        boolean followedByCurrentUser
) {
    public static final RecipeUserInteraction NONE = new RecipeUserInteraction(false, false, false);
}