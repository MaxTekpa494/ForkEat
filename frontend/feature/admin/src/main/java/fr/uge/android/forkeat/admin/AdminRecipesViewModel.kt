package fr.uge.android.forkeat.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.admin.data.api.AdminApi
import fr.uge.android.forkeat.moderator.data.api.ModeratorApi
import fr.uge.android.forkeat.recipes.data.dto.SimpleRecipeDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminRecipesUiState(
    val pendingRecipes: List<SimpleRecipeDTO> = emptyList(),
    val currentPendingRecipesPage : Int = 0,
    val pendingTotal : Int = 0,
    val publishedRecipes: List<SimpleRecipeDTO> = emptyList(),
    val currentPublishedRecipesPage : Int = 0,
    val publishedTotal: Int = 0,
    val isLoading: Boolean = false,
    val actionInProgress: String? = null,   // recipeId en cours d'action
    val error: String? = null,
    val successMessage: String? = null
)

class AdminRecipesViewModel : ViewModel() {

    private val adminService = AdminApi.service
    private val moderatorService = ModeratorApi.service

    private val _uiState = MutableStateFlow(AdminRecipesUiState())
    val uiState: StateFlow<AdminRecipesUiState> = _uiState.asStateFlow()

    init {
        loadPending()
        loadPublished()
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

    fun loadPublished(page: Int = 0, append : Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = adminService.getPublishedRecipes(page)
                if (response.isSuccessful) {
                    val body = response.body()
                    val current = if (append) _uiState.value.publishedRecipes else emptyList()
                    val allRecipes = current + (body?.resources ?: emptyList())
                    _uiState.value = _uiState.value.copy(
                        publishedRecipes = allRecipes,
                        publishedTotal = body?.total ?: 0,
                        currentPublishedRecipesPage = page,
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

    fun loadMorePublishedRecipes() {
        val uiState = _uiState.value
        if(uiState.isLoading || uiState.publishedRecipes.size >= uiState.publishedTotal) return

        val nextPage = uiState.currentPublishedRecipesPage + 1
        loadPublished(nextPage, append = true)
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

    fun unpublishRecipe(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = id, error = null)
            try {
                val response = moderatorService.rejectRecipe(id)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = null,
                        publishedRecipes = _uiState.value.publishedRecipes.filter { it.id != id },
                        successMessage = "Recette dépubliée"
                    )
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
