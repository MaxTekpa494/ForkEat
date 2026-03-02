package fr.uge.android.forkeat.network.dto

data class UserDashboardDTO(val resource: UserDashboardResource)

data class UserDashboardResource(
    val user: UserResource,
    val followerCount: Long,
    val followingCount: Long,
    val totalLikeCount: Long,
    val totalSuperLikeCount: Long,
    val walletBalance: Long,
    val recipeCount: Long
)
