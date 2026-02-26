package fr.uge.android.forkeat.network.dto.admin

data class AdminRecipeStatsDTO(
    val published: Long,
    val pending: Long,
    val draft: Long
)
