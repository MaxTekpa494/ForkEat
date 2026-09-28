package fr.uge.android.forkeat.recipes.data.dto

data class SimpleRecipesListResponse(
    val resources: List<SimpleRecipeDTO>,
    val total: Int
)