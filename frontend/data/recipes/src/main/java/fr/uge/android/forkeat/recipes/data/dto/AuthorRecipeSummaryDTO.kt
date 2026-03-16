package fr.uge.android.forkeat.recipes.data.dto

data class AuthorRecipeSummaryDTO(
    val summary: RecipeSummaryDTO,
    val status: String,
    val rejectionInfo: RecipeRejectionInfoDTO?
)
