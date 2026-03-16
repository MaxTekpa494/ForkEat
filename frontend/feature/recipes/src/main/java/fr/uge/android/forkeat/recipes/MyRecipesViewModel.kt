package fr.uge.android.forkeat.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.recipes.data.api.RecipeApi
import fr.uge.android.forkeat.recipes.data.api.RecipeApiService
import fr.uge.android.forkeat.recipes.data.dto.AuthorRecipeSummaryDTO
import fr.uge.android.forkeat.recipes.data.dto.UserRecipeStatsDTO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class MyRecipesNavigationEvent {
    data class NavigateToEdit(val recipeId: UUID) : MyRecipesNavigationEvent()
    data class NavigateToVariant(val parentId: UUID) : MyRecipesNavigationEvent()
}

data class MyRecipesUiState(
    val recipes: List<AuthorRecipeSummaryDTO> = emptyList(),
    val stats: UserRecipeStatsDTO = UserRecipeStatsDTO(),
    val selectedStatus: String = "PUBLISHED",
    val total: Long = 0,
    val currentPage: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val recipeToDelete: AuthorRecipeSummaryDTO? = null
)

class MyRecipesViewModel : ViewModel() {

    private val api: RecipeApiService = RecipeApi.service
    private val pageSize = 20

    private val _uiState = MutableStateFlow(MyRecipesUiState())
    val uiState: StateFlow<MyRecipesUiState> = _uiState

    private val _navigationEvent = Channel<MyRecipesNavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        loadMyRecipes("PUBLISHED")
    }

    fun onStatusChange(status: String) {
        _uiState.value = _uiState.value.copy(
            selectedStatus = status,
            recipes = emptyList(),
            currentPage = 0,
            total = 0
        )
        loadMyRecipes(status)
    }

    fun loadMyRecipes(status: String = _uiState.value.selectedStatus) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, currentPage = 0)
            try {
                val response = api.getMyRecipes(status = status, page = 0, size = pageSize)
                if (response.isSuccessful) {
                    val body = response.body()?.resource
                    _uiState.value = _uiState.value.copy(
                        recipes = body?.recipes?.items ?: emptyList(),
                        stats = body?.stats ?: UserRecipeStatsDTO(),
                        total = body?.recipes?.total ?: 0,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Erreur (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Erreur : ${e.message}"
                )
            }
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isLoadingMore || currentState.recipes.size >= currentState.total) {
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoadingMore = true)
            try {
                val nextPage = currentState.currentPage + 1
                val response = api.getMyRecipes(
                    status = currentState.selectedStatus,
                    page = nextPage,
                    size = pageSize
                )
                if (response.isSuccessful) {
                    val body = response.body()?.resource
                    val newRecipes = body?.recipes?.items ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        recipes = currentState.recipes + newRecipes,
                        currentPage = nextPage,
                        isLoadingMore = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun onEditRecipe(recipe: AuthorRecipeSummaryDTO) {
        viewModelScope.launch {
            _navigationEvent.send(MyRecipesNavigationEvent.NavigateToEdit(recipe.summary.id))
        }
    }

    fun onCreateVariant(recipe: AuthorRecipeSummaryDTO) {
        viewModelScope.launch {
            _navigationEvent.send(MyRecipesNavigationEvent.NavigateToVariant(recipe.summary.id))
        }
    }

    fun requestDelete(recipe: AuthorRecipeSummaryDTO) {
        _uiState.value = _uiState.value.copy(recipeToDelete = recipe)
    }

    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(recipeToDelete = null)
    }

    fun confirmDelete() {
        val recipe = _uiState.value.recipeToDelete ?: return
        _uiState.value = _uiState.value.copy(recipeToDelete = null)
        viewModelScope.launch {
            try {
                val response = api.deleteRecipe(recipe.summary.id)
                if (response.isSuccessful) {
                    loadMyRecipes()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Suppression échouée (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Erreur : ${e.message}"
                )
            }
        }
    }
}
