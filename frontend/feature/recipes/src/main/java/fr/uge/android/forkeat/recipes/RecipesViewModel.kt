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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedAllergens = MutableStateFlow<Set<String>>(emptySet())
    val selectedAllergens: StateFlow<Set<String>> = _selectedAllergens

    private val _availableAllergens = MutableStateFlow<List<String>>(emptyList())
    val availableAllergens: StateFlow<List<String>> = _availableAllergens

    init {
        loadRecipes(0)
    }

    fun setPageSize(size: Int) {
        _pageSize.value = size
        loadRecipes(_currentPage.value, size)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSearchSubmit() {
        loadRecipes(0)
    }

    fun toggleAllergen(allergen: String) {
        val current = _selectedAllergens.value.toMutableSet()
        if (current.contains(allergen)) {
            current.remove(allergen)
        } else {
            current.add(allergen)
        }
        _selectedAllergens.value = current
        loadRecipes(0)
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedAllergens.value = emptySet()
        loadRecipes(0)
    }

    fun loadRecipes(page: Int, size: Int = _pageSize.value) {
        viewModelScope.launch {
            try {
                val search = _searchQuery.value.ifBlank { null }
                val allergens = _selectedAllergens.value.toList().ifEmpty { null }
                val response = api.getRecipes(
                    page = page,
                    size = size,
                    search = search,
                    allergens = allergens
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    _recipes.value = body?.resources ?: emptyList()
                    _totalCount.value = body?.total ?: 0
                    _currentPage.value = page
                    _errorMessage.value = null
                    updateAvailableAllergens(body?.resources ?: emptyList())
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

    private fun updateAvailableAllergens(recipes: List<RecipeDTO>) {
        val newAllergens = recipes
            .flatMap { it.allergens }
            .map { it.name }
            .distinct()
        val current = _availableAllergens.value.toMutableSet()
        current.addAll(newAllergens)
        _availableAllergens.value = current.sorted()
    }
}
