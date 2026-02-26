package fr.uge.android.forkeat.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.network.dto.admin.AdminRecipeStatsDTO
import fr.uge.android.forkeat.network.dto.admin.AdminUserStatsDTO
import fr.uge.android.forkeat.network.dto.admin.PlatformWalletDTO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val userStats: AdminUserStatsDTO? = null,
    val recipeStats: AdminRecipeStatsDTO? = null,
    val benefitsWallet: PlatformWalletDTO? = null,
    val redistributionWallet: PlatformWalletDTO? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AdminDashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val userStatsDeferred        = async { ForkEatApi.adminService.getUserStats() }
                val recipeStatsDeferred      = async { ForkEatApi.adminService.getRecipeStats() }
                val benefitsDeferred         = async { ForkEatApi.adminService.getBenefitsWallet() }
                val redistributionDeferred   = async { ForkEatApi.adminService.getRedistributionWallet() }

                val userStats      = userStatsDeferred.await()
                val recipeStats    = recipeStatsDeferred.await()
                val benefits       = benefitsDeferred.await()
                val redistribution = redistributionDeferred.await()

                _uiState.value = _uiState.value.copy(
                    userStats        = if (userStats.isSuccessful) userStats.body() else null,
                    recipeStats      = if (recipeStats.isSuccessful) recipeStats.body() else null,
                    benefitsWallet   = if (benefits.isSuccessful) benefits.body() else null,
                    redistributionWallet = if (redistribution.isSuccessful) redistribution.body() else null,
                    isLoading        = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erreur de chargement : ${e.message}"
                )
            }
        }
    }
}
