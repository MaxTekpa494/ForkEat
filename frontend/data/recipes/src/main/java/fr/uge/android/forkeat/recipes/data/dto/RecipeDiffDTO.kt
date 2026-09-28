package fr.uge.android.forkeat.recipes.data.dto

data class RecipeDiffDTO(
    val titleChanged: Boolean = false,
    val originalTitle: String? = null,
    val summaryChanged: Boolean = false,
    val originalSummary: String? = null,
    val timeDelta: Int = 0,
    val imageChanged: Boolean = false,
    val ingredients: List<IngredientDiffDTO> = emptyList(),
    val steps: List<StepDiffDTO> = emptyList(),
    val allergens: List<AllergenDiffDTO> = emptyList(),
    val dietaryFlags: List<DietaryFlagDiffDTO> = emptyList()
) {
    enum class DiffType { UNCHANGED, ADDED, REMOVED, MODIFIED }

    data class IngredientDiffDTO(
        val type: DiffType = DiffType.UNCHANGED,
        val name: String = "",
        val quantity: Double = 0.0,
        val unit: String = "",
        val originalQuantity: Double = 0.0,
        val originalUnit: String = ""
    )

    data class StepDiffDTO(
        val type: DiffType = DiffType.UNCHANGED,
        val stepNumber: Int = 0,
        val instruction: String = "",
        val originalInstruction: String? = null
    )

    data class AllergenDiffDTO(
        val type: DiffType = DiffType.UNCHANGED,
        val name: String = "",
        val severity: String? = null
    )

    data class DietaryFlagDiffDTO(
        val type: DiffType = DiffType.UNCHANGED,
        val flagName: String = ""
    )
}
