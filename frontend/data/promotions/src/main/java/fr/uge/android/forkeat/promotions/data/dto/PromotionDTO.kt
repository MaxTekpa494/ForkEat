package fr.uge.android.forkeat.promotions.data.dto

data class PromotionDTO(
    val id: String,
    val name: String,
    val startsAt: String?,
    val endsAt: String?,
    val priceCents: Long,
    val bonusEveryN: Int?,
    val status: String,
    val createdAt: String?
)
