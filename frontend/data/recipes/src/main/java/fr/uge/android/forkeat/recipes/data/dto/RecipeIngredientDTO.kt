package fr.uge.android.forkeat.recipes.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecipeIngredientDTO(
  val name: String,
  val quantity: Double,
  val unit: String,
)



