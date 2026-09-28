package fr.uge.android.forkeat.promotions.data.dto

data class PromotionListResponse<T>(
    val resources: List<T>,
    val total: Int
)
