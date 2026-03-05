package fr.uge.android.forkeat.moderator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.moderator.data.api.ModeratorApi
import fr.uge.android.forkeat.recipes.data.dto.SimpleRecipeDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ModeratorRecipesUiState(
    val pendingRecipes: List<SimpleRecipeDTO> = emptyList(),
    val currentPendingRecipesPage : Int = 0,
    val pendingTotal : Int = 0,
    val isLoading: Boolean = false,
    val actionInProgress: String? = null,   // recipeId en cours d'action
    val error: String? = null,
    val successMessage: String? = null
)

class ModeratorRecipesViewModel : ViewModel() {

    private val moderatorService = ModeratorApi.service

    private val _uiState = MutableStateFlow(ModeratorRecipesUiState())
    val uiState: StateFlow<ModeratorRecipesUiState> = _uiState.asStateFlow()

    init {
        loadPending()
    }

    fun loadPending(page : Int = 0, size: Int = 10, append : Boolean = false){
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = moderatorService.getPendingRecipes(page, size)
                if (response.isSuccessful) {
                    val body = response.body()
                    val current = if (append) _uiState.value.pendingRecipes else emptyList()
                    val allRecipes = current + (body?.resources ?: emptyList())
                    _uiState.value = _uiState.value.copy(
                        pendingRecipes = allRecipes,
                        pendingTotal = body?.total ?: 0,
                        currentPendingRecipesPage = page,
                        isLoading = false,
                        error = null
                    )
                } else {
                    val code = response.code()
                    val error = when {
                        code >= 500 -> "Le serveur est indisponible, veuillez réessayer plus tard."
                        code in 400..499 -> "Une erreur est survenue, veuillez réessayer."
                        else -> "Une erreur inconnue est survenue."
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error
                    )
                }
            } catch (_: Exception) {
                val error = "Le serveur est indisponible, veuillez réessayer plus tard."
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error
                )
            }
        }
    }

    fun loadMorePendingRecipes() {
        val uiState = _uiState.value
        if(uiState.isLoading || uiState.pendingRecipes.size >= uiState.pendingTotal) return

        val nextPage = uiState.currentPendingRecipesPage + 1
        loadPending(nextPage, append = true)
    }

    fun validateRecipe(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = id, error = null)
            try {
                val response = moderatorService.validateRecipe(id)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = null,
                        pendingRecipes = _uiState.value.pendingRecipes.filter { it.id != id },
                        successMessage = "Recette publiée avec succès"
                    )
                    loadPending()
                } else {
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = null,
                        error = "Erreur lors de la validation : ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    actionInProgress = null,
                    error = "Erreur : ${e.message}"
                )
            }
        }
    }

    fun rejectRecipe(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = id, error = null)
            try {
                val response = moderatorService.rejectRecipe(id)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = null,
                        pendingRecipes = _uiState.value.pendingRecipes.filter { it.id != id },
                        successMessage = "Recette rejetée"
                    )
                    loadPending()
                } else {
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = null,
                        error = "Erreur lors du rejet : ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    actionInProgress = null,
                    error = "Erreur : ${e.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null, error = null)
    }
}
