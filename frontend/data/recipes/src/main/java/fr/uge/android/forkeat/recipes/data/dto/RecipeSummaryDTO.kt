package fr.uge.android.forkeat.recipes.data.dto

import java.util.UUID
import kotlin.time.Instant

data class RecipeSummaryDTO(
    val id: UUID,
    val title: String,
    val summary: String,
    val imageUrl: String?,
    val preparationMinutes: Int,
    val createdAt: Instant,
    val authorUsername: String
)
