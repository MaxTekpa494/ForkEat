package fr.uge.forkeat.service.model.user.projection;

public record UserSocialStats(
        long followerCount,
        long followingCount,
        long totalLikeCount,
        long totalSuperLikeCount
) {
}