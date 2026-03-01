package fr.uge.android.forkeat.recipes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.recipes.data.dto.RecipeDTO
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.network.TokenManager
import fr.uge.android.forkeat.recipes.data.api.RecipeApi
import fr.uge.android.forkeat.recipes.data.api.RecipeApiService
import fr.uge.android.forkeat.recipes.data.dto.RecipeDiffDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeDetailsDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID


class RecipesViewModel(application: Application) : AndroidViewModel(application) {

    private val api: RecipeApiService = RecipeApi.service



    private val tokenManager = TokenManager(application)
    private val _recipes = MutableStateFlow<List<RecipeDTO>>(emptyList())
    val recipes: StateFlow<List<RecipeDTO>> = _recipes

    private val _recipeIds = mutableSetOf<UUID>()

    private val _currentRecipe = MutableStateFlow<RecipeDetailsDTO?>(null)
    val currentRecipe: StateFlow<RecipeDetailsDTO?> = _currentRecipe

    private val _currentParent = MutableStateFlow<RecipeDTO?>(null)
    val currentParent: StateFlow<RecipeDTO?> = _currentParent

    private val _currentDiff = MutableStateFlow<RecipeDiffDTO?>(null)
    val currentDiff: StateFlow<RecipeDiffDTO?> = _currentDiff


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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

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

    fun likeRecipe(recipeId: UUID){
        viewModelScope.launch {
            try {
                val response = api.likeRecipe(
                    token = tokenManager.getToken().toString(),
                    id = recipeId
                )
                if (response.isSuccessful) {

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

    fun unlikeRecipe(recipeId: UUID){
        viewModelScope.launch {
            try {
                val response = api.unlikeRecipe(
                    token = tokenManager.getToken().toString(),
                    id = recipeId
                )
                if (response.isSuccessful) {

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

    fun loadRecipes(page: Int, size: Int = _pageSize.value, append: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                var token = "";
                if(tokenManager.getToken() != null){
                    token = tokenManager.getToken().toString()
                }
                val search = _searchQuery.value.ifBlank { null }
                val allergens = _selectedAllergens.value.toList().ifEmpty { null }
                val response = api.getRecipes(
                    token = token.toString() ,
                    page = page,
                    size = size,
                    search = search,
                    allergens = allergens
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (append) {
                        val current = _recipes.value.toMutableList()
                        val newRecipes = (body?.resources ?: emptyList()).filter { _recipeIds.add(it.id) }
                        val allRecipes = current + newRecipes
                        _recipes.value = if (body?.total != null) allRecipes.take(body.total) else allRecipes
                    } else {
                        val newList = body?.resources ?: emptyList()
                        _recipes.value = newList
                        _recipeIds.clear()
                        _recipeIds.addAll(newList.map { it.id })
                    }
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
            } finally {
              _isLoading.value = false
            }
        }
    }

    fun loadRecipeWithId(id: UUID) {
        viewModelScope.launch {
            _currentRecipe.value = null
            _currentParent.value = null
            _currentDiff.value = null
            try {
                val token = tokenManager.getToken()?.toString() ?: ""
                val response = api.getRecipeWithId(token = token, id = id)
                if (response.isSuccessful) {
                    val data = response.body()?.resource
                    _currentRecipe.value = data?.recipe
                    _currentParent.value = data?.parent
                    _currentDiff.value = data?.diff
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

    fun deleteRecipe(id: UUID, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.deleteRecipe(id)
                if (response.isSuccessful) {
                    _currentRecipe.value = null
                    _currentParent.value = null
                    _currentDiff.value = null
                    loadRecipes(0)
                    onSuccess()
                } else {
                    _errorMessage.value = "Erreur lors de la suppression (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erreur : ${e.message}"
            }
        }
    }

    fun loadMoreRecipes() {
        if(_isLoading.value || _recipes.value.size >= _totalCount.value) return

        val nextPage = _currentPage.value + 1
        if (_recipes.value.size < _totalCount.value) {
            loadRecipes(nextPage, append = true)
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
