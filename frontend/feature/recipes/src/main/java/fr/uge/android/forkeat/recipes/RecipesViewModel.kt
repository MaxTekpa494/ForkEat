package fr.uge.android.forkeat.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.recipes.data.dto.RecipeDTO
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.recipes.data.api.RecipeApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecipesViewModel : ViewModel() {
    private val api: RecipeApiService = ForkEatApi.recipeService

    private val _recipes = MutableStateFlow<List<RecipeDTO>>(emptyList())
    val recipes: StateFlow<List<RecipeDTO>> = _recipes

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _pageSize = MutableStateFlow(10)
    val pageSize: StateFlow<Int> = _pageSize

    init {
        loadRecipes(0)
    }

    fun setPageSize(size: Int) {
        _pageSize.value = size
        loadRecipes(_currentPage.value, size)
    }

    fun loadRecipes(page: Int, size: Int = _pageSize.value) {
        viewModelScope.launch {
            try {
                val response = api.getRecipes(page = page, size = size)
                if (response.isSuccessful) {
                    val body = response.body()
                    _recipes.value = body?.resources ?: emptyList()
                    _totalCount.value = body?.total ?: 0
                    _currentPage.value = page
                    _errorMessage.value = null
                } else {
                    val code = response.code()
                    _errorMessage.value = when {
                        code >= 500 -> "Le serveur est indisponible, veuillez réessayer plus tard."
                        code in 400..499 -> "Une erreur est survenue, veuillez réessayer."
                        else -> "Une erreur inconnue est survenue."
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Le serveur est indisponible, veuillez réessayer plus tard."
            }
        }
    }
}
