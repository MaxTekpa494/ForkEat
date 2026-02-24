package fr.uge.forkeat.service.model.recipe;

public record RecipeUserInteraction(
        boolean likedByCurrentUser,
        boolean superLikedByCurrentUser
) {
    public static final RecipeUserInteraction NONE = new RecipeUserInteraction(false, false);
}