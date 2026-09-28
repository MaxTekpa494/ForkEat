package fr.uge.forkeat.infrastructure.persistence.neo4j.projection;

public record UserSocialCountsProjection(
        long followerCount,
        long followingCount,
        long totalLikeCount,
        long totalSuperLikeCount
) {
}