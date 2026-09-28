package fr.uge.forkeat.presentation.dto.user;

import fr.uge.forkeat.service.model.user.projection.UserAccountDetails;

import java.util.Objects;

public record UserDashboardDTO(
        UserDTO user,
        long followerCount,
        long followingCount,
        long totalLikeCount,
        long totalSuperLikeCount,
        long walletBalance,
        long recipeCount
) {
    public UserDashboardDTO {
        Objects.requireNonNull(user);
    }

    public static UserDashboardDTO from(UserAccountDetails details) {
        Objects.requireNonNull(details);
        var user = new UserDTO(
                details.user().username(),
                details.user().firstName(),
                details.user().lastName(),
                details.user().email(),
                details.user().role().name(),
                details.user().status().name(),
                details.user().authMode().name(),
                details.user().emailVerified(),
                details.user().createdAt(),
                details.user().updatedAt()
        );
        return new UserDashboardDTO(
                user,
                details.socialStats().followerCount(),
                details.socialStats().followingCount(),
                details.socialStats().totalLikeCount(),
                details.socialStats().totalSuperLikeCount(),
                details.walletBalance(),
                details.recipeCount()
        );
    }
}