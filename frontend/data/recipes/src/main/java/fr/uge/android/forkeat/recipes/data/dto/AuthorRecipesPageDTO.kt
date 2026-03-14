package fr.uge.android.forkeat.recipes.data.dto

data class AuthorRecipesPageDTO(
    val stats: UserRecipeStatsDTO,
    val recipes: PageResultDTO<AuthorRecipeSummaryDTO>
)
