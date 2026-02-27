package fr.uge.android.forkeat.recipes.data.dto

import java.util.UUID
import kotlin.time.Instant

data class RecipeDetailsDTO(
    val id: UUID,
    val title: String,
    val summary: String,
    val parentId: java.util.UUID? = null,
    val username: String,
    val preparationMinutes: Int,
    val imageUrl: String,
    val status: String,
    val steps: List<RecipeStepDTO>,
    val ingredients: List<RecipeIngredientDTO>,
    val allergens: List<AllergenDTO>,
    val dietaries: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val nbLike: Long,
    val hasLiked: Boolean
)
