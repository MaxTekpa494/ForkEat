package fr.uge.android.forkeat.recipes.data.dto

data class SimpleRecipeDTO(
  val id: String,
  val title: String,
  val summary: String?,
  val username: String,
  val preparationMinutes: Int,
  val imageUrl: String?,
  val status: String,
  val ingredients: List<RecipeIngredientDTO> = emptyList(),
  val allergens: List<RecipeAllergenDTO> = emptyList(),
  val dietaries: List<String> = emptyList(),
  val createdAt: String?,
  val updatedAt: String?
)