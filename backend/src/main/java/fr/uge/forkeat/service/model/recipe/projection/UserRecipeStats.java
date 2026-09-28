package fr.uge.forkeat.service.model.recipe.projection;

public record UserRecipeStats(long published, long draft, long pendingReview, long rejected) {
    public static final UserRecipeStats ZERO = new UserRecipeStats(0, 0, 0, 0);
}