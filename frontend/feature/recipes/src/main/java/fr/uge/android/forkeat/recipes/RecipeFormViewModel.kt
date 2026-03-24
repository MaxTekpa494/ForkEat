package fr.uge.android.forkeat.recipes

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.network.TokenManager
import fr.uge.android.forkeat.recipes.data.api.RecipeApi
import fr.uge.android.forkeat.recipes.data.dto.RecipeAllergenDTO
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
    val steps: List<StepState> = emptyList(),
    val ingredients: List<IngredientState> = emptyList(),
    val selectedAllergenIds: Set<String> = emptySet(),
    val availableAllergens: List<RecipeAllergenDTO> = emptyList(),
    val availableIngredientNames: List<String> = emptyList(),
    val availableDietaries: List<String> = emptyList(),
    val selectedDietaries: Set<String> = emptySet(),
    val currentImageUrl: String? = null, // image existante (edit / variante)
    val imageUri: Uri? = null,            // nouvelle image choisie (galerie ou caméra)
    val cameraUri: Uri? = null,           // URI temporaire créée pour TakePicture
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

    private val api = RecipeApi.service
    private val tokenManager = TokenManager(application)

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
                        availableIngredientNames = formData.ingredients,
                        availableDietaries = formData.dietaries
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
            val token = tokenManager.getToken()?.toString() ?: ""
            val response = api.getRecipeWithId(token, id)
            if (response.isSuccessful) {
                val recipe = response.body()?.resource?.recipe ?: return
                _uiState.value = _uiState.value.copy(
                    title = recipe.title,
                    summary = recipe.summary,
                    preparationMinutes = recipe.preparationMinutes.toString(),
                    steps = recipe.steps.map { StepState(it.instruction) },
                    ingredients = recipe.ingredients.map {
                        IngredientState(it.name, it.quantity.toString(), it.unit)
                    },
                    selectedAllergenIds = recipe.allergens.map { it.id }.toSet(),
                    selectedDietaries = recipe.dietaries.toSet(),
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
    fun onImageSelected(uri: Uri?) { _uiState.value = _uiState.value.copy(imageUri = uri) }

    /** Crée une URI MediaStore vide où TakePicture écrira la photo. */
    fun createCameraUri(context: Context): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "recipe_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        _uiState.value = _uiState.value.copy(cameraUri = uri)
        return uri
    }

    // --- Étapes ---

    fun addStep() {
        _uiState.value = _uiState.value.copy(steps = _uiState.value.steps + StepState())
    }

    fun removeStep(index: Int) {
        val steps = _uiState.value.steps.toMutableList().also { it.removeAt(index) }
        _uiState.value = _uiState.value.copy(steps = steps)
    }

    fun insertStep(afterIndex: Int) {
        val steps = _uiState.value.steps.toMutableList()
        steps.add(afterIndex + 1, StepState())
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

    // --- Régimes alimentaires ---

    fun toggleDietary(name: String) {
        val current = _uiState.value.selectedDietaries.toMutableSet()
        if (!current.add(name)) current.remove(name)
        _uiState.value = _uiState.value.copy(selectedDietaries = current)
    }

    // --- Image ---

    private fun compressImageUri(uri: Uri): File {
        val contentResolver = getApplication<Application>().contentResolver
        val original = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))

        // Redimensionner si la plus grande dimension dépasse 1920px
        val bitmap = if (original.width > 1920 || original.height > 1920) {
            val scale = 1920f / maxOf(original.width, original.height)
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true
            ).also { original.recycle() }
        } else original

        val tempFile = File.createTempFile("recipe_img", ".jpg", getApplication<Application>().cacheDir)
        tempFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        return tempFile
    }

    // --- Soumission ---

    fun submitRecipe(draft: Boolean) {
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
                    draft = draft,
                    steps = state.steps.mapIndexed { i, s -> RecipeStepDTO(i + 1, s.instruction.trim()) },
                    ingredients = state.ingredients
                        .filter { it.name.isNotBlank() }
                        .map { RecipeIngredientDTO(it.name.trim(), it.quantity.toDoubleOrNull() ?: 0.0, it.unit.trim()) },
                    allergens = selectedAllergens,
                    parentId = parentId,
                    imageUrl = if (state.imageUri == null) state.currentImageUrl else null,
                    dietaries = state.selectedDietaries.toList()
                )

                val jsonBody = ForkEatApi.toJson(dto)
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val imagePart = state.imageUri?.let { uri ->
                    val tempFile = compressImageUri(uri)
                    MultipartBody.Part.createFormData(
                        "image", tempFile.name,
                        tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                }

                val response = when (val m = mode) {
                    is RecipeFormMode.Edit -> api.updateRecipe(m.recipeId, jsonBody, imagePart)
                    is RecipeFormMode.CreateVariant -> api.createRecipe(jsonBody, imagePart)
                    is RecipeFormMode.Create -> api.createRecipe(jsonBody, imagePart)
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
