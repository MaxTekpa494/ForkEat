
package fr.uge.android.forkeat.network.dto

import fr.uge.android.forkeat.recipes.data.dto.PersonalizedRecipeSummaryDTO

data class UserProfileResponseDTO(val resource: UserProfileResource)

data class UserProfileResource(
    val profile: UserProfileData,
    val recipes: List<PersonalizedRecipeSummaryDTO>,
    val totalRecipes: Long,
    val currentPage: Int,
    val totalPages: Int,
    val followedByCurrentUser: Boolean
)

data class UserProfileData(
    val publicProfile: UserPublicProfileData,
    val socialStats: UserSocialStatsData
)

data class UserPublicProfileData(
    val username: String,
    val firstName: String,
    val lastName: String
)

data class UserSocialStatsData(
    val followerCount: Long,
    val followingCount: Long,
    val totalLikeCount: Long,
    val totalSuperLikeCount: Long
)

