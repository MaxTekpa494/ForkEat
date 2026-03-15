package fr.uge.android.forkeat.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.recipes.data.api.RecipeApi
import fr.uge.android.forkeat.recipes.data.api.RecipeApiService
import fr.uge.android.forkeat.recipes.data.dto.RecipeDTO
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
    val recipes: List<RecipeDTO> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val recipeToDelete: RecipeDTO? = null
)

class MyRecipesViewModel : ViewModel() {

    private val api: RecipeApiService = RecipeApi.service

    private val _uiState = MutableStateFlow(MyRecipesUiState())
    val uiState: StateFlow<MyRecipesUiState> = _uiState

    private val _navigationEvent = Channel<MyRecipesNavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        loadMyRecipes()
    }

    fun loadMyRecipes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = api.getMyRecipes()
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        recipes = response.body()?.resources ?: emptyList(),
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

    fun onEditRecipe(recipe: RecipeDTO) {
        viewModelScope.launch {
            _navigationEvent.send(MyRecipesNavigationEvent.NavigateToEdit(recipe.id))
        }
    }

    fun onCreateVariant(recipe: RecipeDTO) {
        viewModelScope.launch {
            _navigationEvent.send(MyRecipesNavigationEvent.NavigateToVariant(recipe.id))
        }
    }

    fun requestDelete(recipe: RecipeDTO) {
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
                val response = api.deleteRecipe(recipe.id)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        recipes = _uiState.value.recipes.filter { it.id != recipe.id }
                    )
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
