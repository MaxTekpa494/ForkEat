package fr.uge.android.forkeat.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.random.Random // Added import


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
        // TODO: Fetch initial dashboard data
        // For now, using mock data
        _uiState.value = DashboardUiState(
            firstName = "John",
            balance = 123.45,
            totalRecipes = 10,
            totalLikes = 250,
            followers = 120
        )
    }

    fun onRefreshDashboard() {
        // TODO: Implement logic to refresh dashboard data from backend
        // For now, simulate refresh
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )
        // Simulate network call
        // delay(2000)
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            // Update with potentially new mock data or fetched data
            balance = kotlin.random.Random.nextDouble(100.0, 500.0),
            totalRecipes = kotlin.random.Random.nextInt(10, 50),
            totalLikes = kotlin.random.Random.nextInt(200, 1000),
            followers = kotlin.random.Random.nextInt(50, 200)
        )
    }

    fun navigateToWallet() {
        // TODO: Handle navigation to wallet screen
    }

    fun navigateToCreateRecipe() {
        // TODO: Handle navigation to create recipe screen
    }

    fun navigateToProfile() {
        viewModelScope.launch {
            _navigationEvent.send(DashboardNavigationEvent.NavigateToProfile)
        }
    }
}
