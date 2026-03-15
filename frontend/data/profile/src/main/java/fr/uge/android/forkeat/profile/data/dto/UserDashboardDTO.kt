package fr.uge.android.forkeat.profile.data.dto

import fr.uge.android.forkeat.network.dto.UserResource

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
