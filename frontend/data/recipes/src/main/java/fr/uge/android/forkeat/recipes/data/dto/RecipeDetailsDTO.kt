package fr.uge.android.forkeat.recipes.data.dto

import java.util.UUID

data class RecipeDetailsDTO(
    val id: UUID,
    val title: String,
    val summary: String,
    val parent: RecipeDTO? = null,
    val username: String,
    val preparationMinutes: Int,
    val imageUrl: String,
    val status: String,
    val steps: List<RecipeStepDTO>,
    val ingredients: List<RecipeIngredientDTO>,
    val allergens: List<AllergenDTO>,
    val dietaryFlags: Map<String, Boolean>,
    val createdAt: String,
    val updatedAt: String,
    val nbLike: Long,
    val hasLiked: Boolean
)
