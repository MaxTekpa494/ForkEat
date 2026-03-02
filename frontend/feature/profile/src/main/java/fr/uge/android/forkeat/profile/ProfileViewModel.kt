package fr.uge.android.forkeat.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val followers: Long = 0,
    val following: Long = 0,
    val totalLikes: Long = 0,
    val superLikes: Long = 0,
    val walletBalance: Long = 0,
    val recipeCount: Long = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = ForkEatApi.profileService.getMyProfile()
                if (response.isSuccessful) {
                    val resource = response.body()?.resource
                    if (resource != null) {
                        _uiState.value = _uiState.value.copy(
                            username = resource.user.username,
                            firstName = resource.user.firstName,
                            lastName = resource.user.lastName,
                            followers = resource.followerCount,
                            following = resource.followingCount,
                            totalLikes = resource.totalLikeCount,
                            superLikes = resource.totalSuperLikeCount,
                            walletBalance = resource.walletBalance,
                            recipeCount = resource.recipeCount,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erreur de chargement du profil"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erreur: ${e.message}"
                )
            }
        }
    }
}
