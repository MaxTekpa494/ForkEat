package fr.uge.android.forkeat.recipes.data.dto

data class RecipesListResponse<T>(
    val resources: List<T>,
    val total: Int
)
