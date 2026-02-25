package fr.uge.android.forkeat.recipes.data.dto

data class CreateRecipeFormData(
    val allergens: List<AllergenDTO>,
    val ingredients: List<String>
)
