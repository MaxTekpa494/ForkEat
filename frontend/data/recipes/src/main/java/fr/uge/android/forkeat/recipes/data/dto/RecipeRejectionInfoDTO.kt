package fr.uge.android.forkeat.recipes.data.dto

import kotlin.time.Instant

data class RecipeRejectionInfoDTO(
    val justification: String,
    val rejectedAt: Instant?
)
