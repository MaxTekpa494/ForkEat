package fr.uge.android.forkeat.recipes.data.dto

data class RecipeDataDTO(
    val recipe: RecipeDetailsDTO,
    val diff: RecipeDiffDTO? = null
)
