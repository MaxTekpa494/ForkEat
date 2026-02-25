package fr.uge.android.forkeat.recipes.data.dto

data class RecipeDataDTO(
    val recipe: RecipeDetailsDTO,
    val parent: RecipeDTO? = null,
    val diff: RecipeDiffDTO? = null
)
