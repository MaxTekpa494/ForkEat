package fr.uge.forkeat.presentation.dto.recipe;

import java.util.UUID;

public record PersonalizedRecipeSummaryDTO(
        UUID id,
        String title,
        String summary,
        String imageUrl,
        int preparationMinutes,
        String authorUsername,
        long likeCount,
        long superLikeCount,
        long followCount,
        boolean likedByCurrentUser,
        boolean superLikedByCurrentUser,
        boolean followedByCurrentUser
) {}