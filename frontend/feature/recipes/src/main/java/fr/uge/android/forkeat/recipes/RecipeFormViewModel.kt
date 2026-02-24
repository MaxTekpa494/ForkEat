package fr.uge.android.forkeat.recipes

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.recipes.data.api.RecipeApiService
import fr.uge.android.forkeat.recipes.data.dto.AllergenDTO
import fr.uge.android.forkeat.recipes.data.dto.CreateRecipeDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeIngredientDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeStepDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

// --- Modes ---

sealed class RecipeFormMode {
    object Create : RecipeFormMode()
    data class Edit(val recipeId: UUID) : RecipeFormMode()
    data class CreateVariant(val parentRecipeId: UUID) : RecipeFormMode()
}

// --- Form state sub-models ---

data class StepState(val instruction: String = "")

data class IngredientState(
    val name: String = "",
    val quantity: String = "",
    val unit: String = ""
)

// --- UI state ---

data class RecipeFormUiState(
    val title: String = "",
    val summary: String = "",
    val preparationMinutes: String = "",
    val status: String = "PUBLISHED",
    val steps: List<StepState> = emptyList(),
    val ingredients: List<IngredientState> = emptyList(),
    val selectedAllergenIds: Set<String> = emptySet(),
    val availableAllergens: List<AllergenDTO> = emptyList(),
    val availableIngredientNames: List<String> = emptyList(),
    val currentImageUrl: String? = null, // image existante (edit / variante)
    val imageUri: Uri? = null,            // nouvelle image choisie par l'utilisateur
    val isLoadingFormData: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val resultRecipeId: UUID? = null
)

// --- ViewModel ---

class RecipeFormViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val api: RecipeApiService = ForkEatApi.recipeService

    val mode: RecipeFormMode = when {
        savedStateHandle.get<String>("recipeId") != null ->
            RecipeFormMode.Edit(UUID.fromString(savedStateHandle.get<String>("recipeId")!!))
        savedStateHandle.get<String>("parentId") != null ->
            RecipeFormMode.CreateVariant(UUID.fromString(savedStateHandle.get<String>("parentId")!!))
        else -> RecipeFormMode.Create
    }

    val formTitle: String = when (mode) {
        is RecipeFormMode.Create -> "Créer une recette"
        is RecipeFormMode.Edit -> "Modifier la recette"
        is RecipeFormMode.CreateVariant -> "Créer une variante"
    }

    val submitLabel: String = when (mode) {
        is RecipeFormMode.Create -> "Créer la recette"
        is RecipeFormMode.Edit -> "Enregistrer les modifications"
        is RecipeFormMode.CreateVariant -> "Créer la variante"
    }

    private val _uiState = MutableStateFlow(RecipeFormUiState())
    val uiState: StateFlow<RecipeFormUiState> = _uiState

    init {
        viewModelScope.launch { loadInitialData() }
    }

    private suspend fun loadInitialData() {
        _uiState.value = _uiState.value.copy(isLoadingFormData = true)

        // Charger les allergènes et noms d'ingrédients disponibles
        try {
            val response = api.getCreateRecipeData()
            if (response.isSuccessful) {
                response.body()?.resource?.let { formData ->
                    _uiState.value = _uiState.value.copy(
                        availableAllergens = formData.allergens,
                        availableIngredientNames = formData.ingredients
                    )
                }
            }
        } catch (_: Exception) { /* on continue sans les données de formulaire */ }

        // Pré-remplir selon le mode
        when (val m = mode) {
            is RecipeFormMode.Edit -> prefillFromRecipe(m.recipeId, isVariant = false)
            is RecipeFormMode.CreateVariant -> prefillFromRecipe(m.parentRecipeId, isVariant = true)
            is RecipeFormMode.Create -> Unit
        }

        _uiState.value = _uiState.value.copy(isLoadingFormData = false)
    }

    private suspend fun prefillFromRecipe(id: UUID, isVariant: Boolean) {
        try {
            val response = api.getRecipe(id)
            if (response.isSuccessful) {
                val recipe = response.body()?.resource ?: return
                _uiState.value = _uiState.value.copy(
                    title = recipe.title,
                    summary = recipe.summary,
                    preparationMinutes = recipe.preparationMinutes.toString(),
                    status = if (isVariant) "PUBLISHED" else recipe.status,
                    steps = recipe.steps.map { StepState(it.instruction) },
                    ingredients = recipe.ingredients.map {
                        IngredientState(it.name, it.quantity.toString(), it.unit)
                    },
                    selectedAllergenIds = recipe.allergens.map { it.id }.toSet(),
                    currentImageUrl = recipe.imageUrl
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Impossible de charger la recette"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = "Erreur : ${e.message}")
        }
    }

    // --- Champs simples ---

    fun onTitleChange(v: String) { _uiState.value = _uiState.value.copy(title = v) }
    fun onSummaryChange(v: String) { _uiState.value = _uiState.value.copy(summary = v) }
    fun onPrepMinutesChange(v: String) { _uiState.value = _uiState.value.copy(preparationMinutes = v) }
    fun onStatusChange(v: String) { _uiState.value = _uiState.value.copy(status = v) }
    fun onImageSelected(uri: Uri?) { _uiState.value = _uiState.value.copy(imageUri = uri) }

    // --- Étapes ---

    fun addStep() {
        _uiState.value = _uiState.value.copy(steps = _uiState.value.steps + StepState())
    }

    fun removeStep(index: Int) {
        val steps = _uiState.value.steps.toMutableList().also { it.removeAt(index) }
        _uiState.value = _uiState.value.copy(steps = steps)
    }

    fun updateStep(index: Int, instruction: String) {
        val steps = _uiState.value.steps.toMutableList().also { it[index] = StepState(instruction) }
        _uiState.value = _uiState.value.copy(steps = steps)
    }

    // --- Ingrédients ---

    fun addIngredient() {
        _uiState.value = _uiState.value.copy(ingredients = _uiState.value.ingredients + IngredientState())
    }

    fun removeIngredient(index: Int) {
        val ingredients = _uiState.value.ingredients.toMutableList().also { it.removeAt(index) }
        _uiState.value = _uiState.value.copy(ingredients = ingredients)
    }

    fun updateIngredientName(index: Int, v: String) {
        val list = _uiState.value.ingredients.toMutableList().also { it[index] = it[index].copy(name = v) }
        _uiState.value = _uiState.value.copy(ingredients = list)
    }

    fun updateIngredientQuantity(index: Int, v: String) {
        val list = _uiState.value.ingredients.toMutableList().also { it[index] = it[index].copy(quantity = v) }
        _uiState.value = _uiState.value.copy(ingredients = list)
    }

    fun updateIngredientUnit(index: Int, v: String) {
        val list = _uiState.value.ingredients.toMutableList().also { it[index] = it[index].copy(unit = v) }
        _uiState.value = _uiState.value.copy(ingredients = list)
    }

    // --- Allergènes ---

    fun toggleAllergen(allergenId: String) {
        val current = _uiState.value.selectedAllergenIds.toMutableSet()
        if (!current.add(allergenId)) current.remove(allergenId)
        _uiState.value = _uiState.value.copy(selectedAllergenIds = current)
    }

    // --- Soumission ---

    fun submitRecipe() {
        val state = _uiState.value

        if (state.title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Le titre est obligatoire")
            return
        }
        if (state.summary.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Le résumé est obligatoire")
            return
        }
        val prepMinutes = state.preparationMinutes.toIntOrNull()
        if (prepMinutes == null || prepMinutes <= 0) {
            _uiState.value = state.copy(errorMessage = "Le temps de préparation doit être un nombre positif")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            try {
                val selectedAllergens = state.availableAllergens
                    .filter { state.selectedAllergenIds.contains(it.id) }

                val parentId = if (mode is RecipeFormMode.CreateVariant) {
                    mode.parentRecipeId.toString()
                } else null

                val dto = CreateRecipeDTO(
                    title = state.title.trim(),
                    summary = state.summary.trim(),
                    preparationMinutes = prepMinutes,
                    status = state.status,
                    steps = state.steps.mapIndexed { i, s -> RecipeStepDTO(i + 1, s.instruction.trim()) },
                    ingredients = state.ingredients
                        .filter { it.name.isNotBlank() }
                        .map { RecipeIngredientDTO(it.name.trim(), it.quantity.toDoubleOrNull() ?: 0.0, it.unit.trim()) },
                    allergens = selectedAllergens,
                    parentId = parentId
                )

                val jsonBody = ForkEatApi.toJson(dto)
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val imagePart = state.imageUri?.let { uri ->
                    val contentResolver = getApplication<Application>().contentResolver
                    val inputStream = contentResolver.openInputStream(uri)
                    val tempFile = File.createTempFile("recipe_img", ".jpg", getApplication<Application>().cacheDir)
                    tempFile.outputStream().use { out -> inputStream?.copyTo(out) }
                    MultipartBody.Part.createFormData(
                        "image", tempFile.name,
                        tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                }

                val response = when (val m = mode) {
                    is RecipeFormMode.Edit -> api.updateRecipe(m.recipeId, jsonBody, imagePart)
                    else -> api.createRecipe(jsonBody, imagePart)
                }

                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        resultRecipeId = response.body()?.resource?.id
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = "Erreur (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "Erreur : ${e.message}"
                )
            }
        }
    }

    fun onNavigated() {
        _uiState.value = _uiState.value.copy(resultRecipeId = null)
    }
}
