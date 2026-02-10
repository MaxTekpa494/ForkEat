package fr.uge.android.forkeat.recipes.data.dto

data class RecipesListResponse(
    val resources: List<RecipeDTO>,
    val total: Int
)