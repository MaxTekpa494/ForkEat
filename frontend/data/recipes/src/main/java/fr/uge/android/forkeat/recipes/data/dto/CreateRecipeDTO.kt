package fr.uge.android.forkeat.recipes.data.dto

data class CreateRecipeDTO(
    val title: String,
    val summary: String,
    val preparationMinutes: Int,
    val status: String,
    val steps: List<RecipeStepDTO>,
    val ingredients: List<RecipeIngredientDTO>,
    val allergens: List<AllergenDTO>,
    val parentId: String? = null,
    val dietaryFlags: Map<String, Boolean> = emptyMap()
)
