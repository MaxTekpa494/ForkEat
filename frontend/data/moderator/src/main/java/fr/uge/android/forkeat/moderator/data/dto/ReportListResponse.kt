package fr.uge.android.forkeat.moderator.data.dto

data class ReportListResponse<T>(
    val resources: List<T>,
    val total: Int
)
