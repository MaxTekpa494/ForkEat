package fr.uge.android.forkeat.recipes.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecipeStepDTO(
    val stepNumber: Int,
    val instruction: String
)

