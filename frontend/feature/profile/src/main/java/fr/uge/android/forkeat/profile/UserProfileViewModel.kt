package fr.uge.android.forkeat.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.network.TokenManager
import fr.uge.android.forkeat.recipes.data.dto.PersonalizedRecipeSummaryDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class UserProfileUiState(
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val followerCount: Long = 0,
    val followingCount: Long = 0,
    val totalLikeCount: Long = 0,
    val recipes: List<PersonalizedRecipeSummaryDTO> = emptyList(),
    val totalRecipes: Long = 0,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val followedByCurrentUser: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private val tokenManager = TokenManager(application)
    private var targetUsername: String = ""

    fun loadProfile(username: String, page: Int = 0) {
        targetUsername = username
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = ForkEatApi.profileService.getUserProfile(username, page)
                if (response.isSuccessful) {
                    val r = response.body()?.resource ?: return@launch
                    val newRecipes = if (page == 0) r.recipes else _uiState.value.recipes + r.recipes
                    _uiState.value = UserProfileUiState(
                        username = r.profile.publicProfile.username,
                        firstName = r.profile.publicProfile.firstName,
                        lastName = r.profile.publicProfile.lastName,
                        followerCount = r.profile.socialStats.followerCount,
                        followingCount = r.profile.socialStats.followingCount,
                        totalLikeCount = r.profile.socialStats.totalLikeCount,
                        recipes = newRecipes,
                        totalRecipes = r.totalRecipes,
                        currentPage = r.currentPage,
                        totalPages = r.totalPages,
                        followedByCurrentUser = r.followedByCurrentUser,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Profil introuvable")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Erreur: ${e.message}")
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.currentPage < state.totalPages - 1 && !state.isLoading) {
            loadProfile(targetUsername, state.currentPage + 1)
        }
    }

    fun likeRecipe(recipeId: UUID) {
        viewModelScope.launch {
            updateRecipeInList(recipeId, liked = true)
            try {
                val token = tokenManager.getToken() ?: ""
                val response = ForkEatApi.recipeService.likeRecipe(token = token, id = recipeId)
                if (!response.isSuccessful) {
                    updateRecipeInList(recipeId, liked = false)
                }
            } catch (e: Exception) {
                updateRecipeInList(recipeId, liked = false)
            }
        }
    }

    fun unlikeRecipe(recipeId: UUID) {
        viewModelScope.launch {
            updateRecipeInList(recipeId, liked = false)
            try {
                val token = tokenManager.getToken() ?: ""
                val response = ForkEatApi.recipeService.unlikeRecipe(token = token, id = recipeId)
                if (!response.isSuccessful) {
                    updateRecipeInList(recipeId, liked = true)
                }
            } catch (e: Exception) {
                updateRecipeInList(recipeId, liked = true)
            }
        }
    }

    private fun updateRecipeInList(recipeId: UUID, liked: Boolean) {
        _uiState.value = _uiState.value.copy(
            recipes = _uiState.value.recipes.map { recipe ->
                if (recipe.id == recipeId) {
                    if (recipe.likedByCurrentUser != liked) {
                        recipe.copy(
                            likedByCurrentUser = liked,
                            likeCount = if (liked) recipe.likeCount + 1 else recipe.likeCount - 1
                        )
                    } else recipe
                } else recipe
            }
        )
    }
}
