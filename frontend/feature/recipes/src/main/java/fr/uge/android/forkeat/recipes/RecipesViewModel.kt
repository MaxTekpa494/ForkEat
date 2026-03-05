package fr.uge.android.forkeat.recipes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.network.TokenManager
import fr.uge.android.forkeat.recipes.data.api.RecipeApiService
import fr.uge.android.forkeat.recipes.data.dto.AllergenDTO
import fr.uge.android.forkeat.recipes.data.dto.PersonalizedRecipeSummaryDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeDetailsDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeDiffDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class RecipesViewModel(application: Application) : AndroidViewModel(application) {

    private val api: RecipeApiService = ForkEatApi.recipeService
    private val tokenManager = TokenManager(application)

    private val _recipes = MutableStateFlow<List<PersonalizedRecipeSummaryDTO>>(emptyList())
    val recipes: StateFlow<List<PersonalizedRecipeSummaryDTO>> = _recipes.asStateFlow()

    private val _recipeIds = mutableSetOf<UUID>()

    private val _currentRecipe = MutableStateFlow<RecipeDetailsDTO?>(null)
    val currentRecipe: StateFlow<RecipeDetailsDTO?> = _currentRecipe.asStateFlow()

    private val _currentParent = MutableStateFlow<RecipeDTO?>(null)
    val currentParent: StateFlow<RecipeDTO?> = _currentParent.asStateFlow()

    private val _currentDiff = MutableStateFlow<RecipeDiffDTO?>(null)
    val currentDiff: StateFlow<RecipeDiffDTO?> = _currentDiff.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _emailNotVerified = MutableStateFlow(false)
    val emailNotVerified: StateFlow<Boolean> = _emailNotVerified.asStateFlow()

    fun dismissEmailNotVerified() { _emailNotVerified.value = false }

    private val _insufficientFunds = MutableStateFlow(false)
    val insufficientFunds: StateFlow<Boolean> = _insufficientFunds.asStateFlow()

    fun dismissInsufficientFunds() { _insufficientFunds.value = false }

    private val _currentPage = MutableStateFlow(0)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _pageSize = MutableStateFlow(10)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAllergens = MutableStateFlow<Set<String>>(emptySet())
    val selectedAllergens: StateFlow<Set<String>> = _selectedAllergens.asStateFlow()

    private val _availableAllergens = MutableStateFlow<List<AllergenDTO>>(emptyList())
    val availableAllergens: StateFlow<List<AllergenDTO>> = _availableAllergens.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadRecipes(0)
        loadAllergens()
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

    fun likeRecipe(recipeId: UUID) {
        viewModelScope.launch {
            updateRecipeStates(recipeId, liked = true)
            try {
                val token = tokenManager.getToken() ?: ""
                val response = api.likeRecipe(token = token, id = recipeId)
                if (!response.isSuccessful) {
                    updateRecipeStates(recipeId, liked = false)
                    handleError(response.code())
                }
            } catch (e: Exception) {
                updateRecipeStates(recipeId, liked = false)
                _errorMessage.value = "Erreur réseau : ${e.message}"
            }
        }
    }

    fun unlikeRecipe(recipeId: UUID) {
        viewModelScope.launch {
            updateRecipeStates(recipeId, liked = false)
            try {
                val token = tokenManager.getToken() ?: ""
                val response = api.unlikeRecipe(token = token, id = recipeId)
                if (!response.isSuccessful) {
                    updateRecipeStates(recipeId, liked = true)
                    handleError(response.code())
                }
            } catch (e: Exception) {
                updateRecipeStates(recipeId, liked = true)
                _errorMessage.value = "Erreur réseau : ${e.message}"
            }
        }
    }

    private fun updateRecipeStates(recipeId: UUID, liked: Boolean) {
        // Update main list
        _recipes.value = _recipes.value.map { recipe ->
            if (recipe.id == recipeId) {
                if (recipe.likedByCurrentUser != liked) {
                    recipe.copy(
                        likedByCurrentUser = liked,
                        likeCount = if (liked) recipe.likeCount + 1 else recipe.likeCount - 1
                    )
                } else recipe
            } else recipe
        }
        
        // Update current details if open
        _currentRecipe.value?.let { current ->
            if (current.id == recipeId && current.hasLiked != liked) {
                _currentRecipe.value = current.copy(
                    hasLiked = liked,
                    nbLike = if (liked) current.nbLike + 1 else current.nbLike - 1
                )
            }
        }
    }

    fun followRecipe(recipeId: UUID) {
        viewModelScope.launch {
            updateFollowStates(recipeId, followed = true)
            try {
                val token = tokenManager.getToken() ?: ""
                val response = api.followRecipe(token = token, id = recipeId)
                if (!response.isSuccessful) {
                    updateFollowStates(recipeId, followed = false)
                    handleError(response.code())
                }
            } catch (e: Exception) {
                updateFollowStates(recipeId, followed = false)
                _errorMessage.value = "Erreur réseau : ${e.message}"
            }
        }
    }

    fun unfollowRecipe(recipeId: UUID) {
        viewModelScope.launch {
            updateFollowStates(recipeId, followed = false)
            try {
                val token = tokenManager.getToken() ?: ""
                val response = api.unfollowRecipe(token = token, id = recipeId)
                if (!response.isSuccessful) {
                    updateFollowStates(recipeId, followed = true)
                    handleError(response.code())
                }
            } catch (e: Exception) {
                updateFollowStates(recipeId, followed = true)
                _errorMessage.value = "Erreur réseau : ${e.message}"
            }
        }
    }

    private fun updateFollowStates(recipeId: UUID, followed: Boolean) {
        _recipes.value = _recipes.value.map { recipe ->
            if (recipe.id == recipeId && recipe.followedByCurrentUser != followed) {
                recipe.copy(
                    followedByCurrentUser = followed,
                    followCount = if (followed) recipe.followCount + 1 else recipe.followCount - 1
                )
            } else recipe
        }
        _currentRecipe.value?.let { current ->
            if (current.id == recipeId && current.hasFollowed != followed) {
                _currentRecipe.value = current.copy(
                    hasFollowed = followed,
                    nbFollow = if (followed) current.nbFollow + 1 else current.nbFollow - 1
                )
            }
        }
    }

    fun superLikeRecipe(recipeId: UUID){
        viewModelScope.launch {
            try {
                val response = api.superLikeRecipe(
                    token = tokenManager.getToken().toString(),
                    id = recipeId
                )
                if (response.isSuccessful) {
                    _currentRecipe.value?.let { current ->
                        if (current.id == recipeId && !current.hasSuperLiked) {
                            _currentRecipe.value = current.copy(
                                hasSuperLiked = true,
                                nbSuperLike = current.nbSuperLike + 1
                            )
                        }
                    }
                    _recipes.value = _recipes.value.map { recipe ->
                        if (recipe.id == recipeId && !recipe.superLikedByCurrentUser) {
                            recipe.copy(
                                superLikedByCurrentUser = true,
                                superLikeCount = recipe.superLikeCount + 1
                            )
                        } else recipe
                    }
                } else {
                    handleError(response.code())
                }
            } catch (e: Exception) {
                _errorMessage.value = "Le serveur est indisponible, veuillez réessayer plus tard."
            }
        }
    }

    fun loadRecipes(page: Int, size: Int = _pageSize.value, append: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val token = tokenManager.getToken()
                val search = _searchQuery.value.ifBlank { null }
                val allergens = _selectedAllergens.value.toList().ifEmpty { null }

                val response = api.getRecipes(
                    token = token,
                    page = page,
                    size = size,
                    search = search,
                    allergens = allergens
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val newItems = body?.resources ?: emptyList()

                    if (append) {
                        val filteredNewItems = newItems.filter { _recipeIds.add(it.id) }
                        _recipes.value += filteredNewItems
                    } else {
                        _recipeIds.clear()
                        _recipeIds.addAll(newItems.map { it.id })
                        _recipes.value = newItems
                    }

                    _totalCount.value = body?.total ?: 0
                    _currentPage.value = page
                } else {
                    handleError(response.code())
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erreur réseau : ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAllergens() {
        viewModelScope.launch {
            try {
                val response = api.getAllergens()
                if (response.isSuccessful) {
                    _availableAllergens.value = response.body()?.resources ?: emptyList()
                }
            } catch (_: Exception) {
                // Silently fail for allergens or log it
            }
        }
    }

    fun loadRecipeWithId(id: UUID) {
        viewModelScope.launch {
            _currentRecipe.value = null
            _currentParent.value = null
            _currentDiff.value = null
            try {
                val token = tokenManager.getToken()
                val response = api.getRecipeWithId(token = token, id = id)
                if (response.isSuccessful) {
                    val data = response.body()?.resource
                    _currentRecipe.value = data?.recipe
                    _currentParent.value = data?.parent
                    _currentDiff.value = data?.diff
                    _errorMessage.value = null
                    
                    // Synchronize local state in main list
                    data?.recipe?.let { recipe ->
                        updateRecipeStates(recipe.id, recipe.hasLiked)
                    }
                } else {
                    handleError(response.code())
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erreur réseau : ${e.message}"
            }
        }
    }

    fun deleteRecipe(id: UUID, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.deleteRecipe(id)
                if (response.isSuccessful) {
                    _recipes.value = _recipes.value.filter { it.id != id }
                    onSuccess()
                } else {
                    _errorMessage.value = "Erreur lors de la suppression (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erreur réseau : ${e.message}"
            }
        }
    }

    fun loadMoreRecipes() {
        if (_isLoading.value || _recipes.value.size >= _totalCount.value) return
        loadRecipes(_currentPage.value + 1, append = true)
    }

    private fun handleError(code: Int) {
        if (code == 403) {
            _emailNotVerified.value = true
            return
        }
        if (code == 402) {
            _insufficientFunds.value = true
            return
        }
        _errorMessage.value = when {
            code >= 500 -> "Le serveur est indisponible, veuillez réessayer plus tard."
            code in 400..499 -> "Une erreur est survenue, veuillez vérifier votre connexion."
            else -> "Une erreur inconnue est survenue ($code)."
        }
    }
}
