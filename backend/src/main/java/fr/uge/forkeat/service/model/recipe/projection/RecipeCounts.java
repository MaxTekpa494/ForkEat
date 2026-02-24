package fr.uge.forkeat.service.model.recipe.projection;

public record RecipeCounts(long likeCount, long superLikeCount) {

    public static final RecipeCounts ZERO = new RecipeCounts(0, 0);

    public RecipeCounts {
        if (likeCount < 0) {
            throw new IllegalArgumentException("likeCount cannot be negative");
        }
        if (superLikeCount < 0) {
            throw new IllegalArgumentException("superLikeCount cannot be negative");
        }
    }
}