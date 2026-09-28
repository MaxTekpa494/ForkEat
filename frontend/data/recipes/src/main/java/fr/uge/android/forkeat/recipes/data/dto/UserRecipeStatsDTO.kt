package fr.uge.android.forkeat.recipes.data.dto

data class UserRecipeStatsDTO(
    val published: Long = 0,
    val draft: Long = 0,
    val pendingReview: Long = 0,
    val rejected: Long = 0
)
