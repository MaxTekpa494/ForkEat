package fr.uge.android.forkeat.recipes.data.dto

import java.util.UUID
import kotlin.time.Instant

data class RecipeDTO(
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
  val createdAt: Instant,
  val updatedAt: Instant
)
