package fr.uge.android.forkeat.recipes.data.dto

data class CreateRecipeDTO(
  val title: String,
  val summary: String,
  val preparationMinutes: Int,
  val status: String,
  val steps: List<RecipeStepDTO>,
  val ingredients: List<RecipeIngredientDTO>,
  val allergens: List<RecipeAllergenDTO>,
  val parentId: String? = null,
  val dietaries: List<String> = emptyList()
)
