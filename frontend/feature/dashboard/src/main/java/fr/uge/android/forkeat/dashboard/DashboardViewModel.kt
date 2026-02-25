package fr.uge.android.forkeat.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


sealed class DashboardNavigationEvent {
    data object NavigateToProfile : DashboardNavigationEvent()
    data object NavigateToWallet : DashboardNavigationEvent()
    data object NavigateToCreateRecipe : DashboardNavigationEvent()
}

data class DashboardUiState(
    val firstName: String = "Utilisateur",
    val balance: Double = 0.00,
    val totalRecipes: Int = 0,
    val totalLikes: Int = 0,
    val followers: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class DashboardViewModel : ViewModel() {
    private val _navigationEvent = Channel<DashboardNavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val meDeferred = async { ForkEatApi.authService.me() }
                val balanceDeferred = async { ForkEatApi.walletService.getBalance() }

                val meResponse = meDeferred.await()
                val balanceResponse = balanceDeferred.await()

                val firstName = if (meResponse.isSuccessful) {
                    meResponse.body()?.resource?.firstName ?: "Utilisateur"
                } else "Utilisateur"

                val balanceCents = if (balanceResponse.isSuccessful) {
                    balanceResponse.body()?.balance ?: 0L
                } else 0L

                _uiState.value = _uiState.value.copy(
                    firstName = firstName,
                    balance = balanceCents / 100.0,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erreur de chargement: ${e.message}"
                )
            }
        }
    }

    fun onRefreshDashboard() {
        loadDashboard()
    }

    fun navigateToWallet() {
        viewModelScope.launch {
            _navigationEvent.send(DashboardNavigationEvent.NavigateToWallet)
        }
    }

    fun navigateToCreateRecipe() {
        viewModelScope.launch {
            _navigationEvent.send(DashboardNavigationEvent.NavigateToCreateRecipe)
        }
    }

    fun navigateToProfile() {
        viewModelScope.launch {
            _navigationEvent.send(DashboardNavigationEvent.NavigateToProfile)
        }
    }
}
