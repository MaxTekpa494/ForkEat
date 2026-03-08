package fr.uge.android.forkeat.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.TokenManager
import fr.uge.android.forkeat.profile.data.api.ProfileApi
import fr.uge.android.forkeat.recipes.data.api.RecipeApi
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
    val error: String? = null,
    val insufficientFunds: Boolean = false,
    val emailNotVerified: Boolean = false
)

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val profileService = ProfileApi.service
    private val recipeService = RecipeApi.service
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private val tokenManager = TokenManager(application)
    private var targetUsername: String = ""

    fun dismissInsufficientFunds() {
        _uiState.value = _uiState.value.copy(insufficientFunds = false)
    }

    fun dismissEmailNotVerified() {
        _uiState.value = _uiState.value.copy(emailNotVerified = false)
    }

    fun loadProfile(username: String, page: Int = 0) {
        targetUsername = username
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = profileService.getUserProfile(username, page)
                if (response.isSuccessful) {
                    val r = response.body()?.resource ?: return@launch
                    val newRecipes = if (page == 0) r.recipes else _uiState.value.recipes + r.recipes
                    _uiState.value = _uiState.value.copy(
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
                val response = recipeService.likeRecipe(token = token, id = recipeId)
                if (!response.isSuccessful) {
                    updateRecipeInList(recipeId, liked = false)
                    handleError(response.code())
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
                val response = recipeService.unlikeRecipe(token = token, id = recipeId)
                if (!response.isSuccessful) {
                    updateRecipeInList(recipeId, liked = true)
                    handleError(response.code())
                }
            } catch (e: Exception) {
                updateRecipeInList(recipeId, liked = true)
            }
        }
    }

    fun superLikeRecipe(recipeId: UUID) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken() ?: ""
                val response = recipeService.superLikeRecipe(token = token, id = recipeId)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        recipes = _uiState.value.recipes.map { recipe ->
                            if (recipe.id == recipeId && !recipe.superLikedByCurrentUser) {
                                recipe.copy(
                                    superLikedByCurrentUser = true,
                                    superLikeCount = recipe.superLikeCount + 1
                                )
                            } else recipe
                        }
                    )
                } else {
                    handleError(response.code())
                }
            } catch (e: Exception) {
                // Silently fail or log it
            }
        }
    }

    fun followRecipe(recipeId: UUID) {
        viewModelScope.launch {
            updateFollowStates(recipeId, followed = true)
            try {
                val token = tokenManager.getToken() ?: ""
                val response = recipeService.followRecipe(token = token, id = recipeId)
                if (!response.isSuccessful) {
                    updateFollowStates(recipeId, followed = false)
                    handleError(response.code())
                }
            } catch (e: Exception) {
                updateFollowStates(recipeId, followed = false)
            }
        }
    }

    fun unfollowRecipe(recipeId: UUID) {
        viewModelScope.launch {
            updateFollowStates(recipeId, followed = false)
            try {
                val token = tokenManager.getToken() ?: ""
                val response = recipeService.unfollowRecipe(token = token, id = recipeId)
                if (!response.isSuccessful) {
                    updateFollowStates(recipeId, followed = true)
                    handleError(response.code())
                }
            } catch (e: Exception) {
                updateFollowStates(recipeId, followed = true)
            }
        }
    }

    private fun updateFollowStates(recipeId: UUID, followed: Boolean) {
        _uiState.value = _uiState.value.copy(
            recipes = _uiState.value.recipes.map { recipe ->
                if (recipe.id == recipeId && recipe.followedByCurrentUser != followed) {
                    recipe.copy(
                        followedByCurrentUser = followed,
                        followCount = if (followed) recipe.followCount + 1 else recipe.followCount - 1
                    )
                } else recipe
            }
        )
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

    fun follow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                followedByCurrentUser = true,
                followerCount = _uiState.value.followerCount + 1
            )
            try {
                val token = tokenManager.getToken() ?: ""
                val response = profileService.followUser(token, targetUsername)
                if (!response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        followedByCurrentUser = false,
                        followerCount = _uiState.value.followerCount - 1
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    followedByCurrentUser = false,
                    followerCount = _uiState.value.followerCount - 1
                )
            }
        }
    }

    fun unfollow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                followedByCurrentUser = false,
                followerCount = _uiState.value.followerCount - 1
            )
            try {
                val token = tokenManager.getToken() ?: ""
                val response = profileService.unfollowUser(token, targetUsername)
                if (!response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        followedByCurrentUser = true,
                        followerCount = _uiState.value.followerCount + 1
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    followedByCurrentUser = true,
                    followerCount = _uiState.value.followerCount + 1
                )
            }
        }
    }

    private fun handleError(code: Int) {
        if (code == 403) {
            _uiState.value = _uiState.value.copy(emailNotVerified = true)
        } else if (code == 402) {
            _uiState.value = _uiState.value.copy(insufficientFunds = true)
        }
    }
}
