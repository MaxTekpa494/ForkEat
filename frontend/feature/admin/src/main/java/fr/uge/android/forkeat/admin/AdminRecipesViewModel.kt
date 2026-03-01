package fr.uge.android.forkeat.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.admin.data.api.AdminApi
import fr.uge.android.forkeat.admin.data.dto.AdminRecipeDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminRecipesUiState(
    val pendingRecipes: List<AdminRecipeDTO> = emptyList(),
    val publishedRecipes: List<AdminRecipeDTO> = emptyList(),
    val publishedTotal: Long = 0,
    val publishedPage: Int = 0,
    val isLoading: Boolean = false,
    val actionInProgress: String? = null,   // recipeId en cours d'action
    val error: String? = null,
    val successMessage: String? = null
)

class AdminRecipesViewModel : ViewModel() {

    private val adminService = AdminApi.service

    private val _uiState = MutableStateFlow(AdminRecipesUiState())
    val uiState: StateFlow<AdminRecipesUiState> = _uiState.asStateFlow()

    init {
        loadPending()
        loadPublished()
    }

    fun loadPending() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = adminService.getPendingRecipes()
                if (response.isSuccessful) {
                    val body = response.body()
                    _uiState.value = _uiState.value.copy(
                        pendingRecipes = body?.resources ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erreur ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erreur de chargement : ${e.message}"
                )
            }
        }
    }

    fun loadPublished(page: Int = 0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = adminService.getPublishedRecipes(page)
                if (response.isSuccessful) {
                    val body = response.body()
                    _uiState.value = _uiState.value.copy(
                        publishedRecipes = body?.resources ?: emptyList(),
                        publishedTotal = body?.total ?: 0,
                        publishedPage = page,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erreur ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erreur de chargement : ${e.message}"
                )
            }
        }
    }

    fun validateRecipe(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = id, error = null)
            try {
                val response = adminService.validateRecipe(id)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = null,
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
                val response = adminService.rejectRecipe(id)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = null,
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

    fun unpublishRecipe(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = id, error = null)
            try {
                val response = adminService.rejectRecipe(id)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = null,
                        successMessage = "Recette dépubliée"
                    )
                    loadPublished(_uiState.value.publishedPage)
                } else {
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = null,
                        error = "Erreur : ${response.code()}"
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
