package fr.uge.android.forkeat.admin.data.dto

data class CreatePromotionRequest(
    val name: String,
    val startsAt: String,
    val endsAt: String?,
    val priceCents: Long,
    val bonusEveryN: Int?
)
