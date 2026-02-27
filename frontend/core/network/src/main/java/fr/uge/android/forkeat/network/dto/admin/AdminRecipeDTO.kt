package fr.uge.android.forkeat.network.dto.admin

data class AdminRecipeDTO(
    val id: String,
    val title: String,
    val summary: String?,
    val username: String,
    val preparationMinutes: Int,
    val imageUrl: String?,
    val status: String,
    val ingredients: List<AdminRecipeIngredientDTO> = emptyList(),
    val allergens: List<AdminRecipeAllergenDTO> = emptyList(),
    val createdAt: String?,
    val updatedAt: String?
)

data class AdminRecipeIngredientDTO(
    val name: String,
    val quantity: Double,
    val unit: String
)

data class AdminRecipeAllergenDTO(
    val id: String?,
    val name: String,
    val severity: String
)

data class AdminRecipesListResponse(
    val resources: List<AdminRecipeDTO>,
    val total: Long
)
