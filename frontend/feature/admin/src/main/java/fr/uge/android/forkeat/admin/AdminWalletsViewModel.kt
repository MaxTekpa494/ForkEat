package fr.uge.android.forkeat.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.network.dto.admin.PlatformWalletDTO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminWalletsUiState(
    val benefitsWallet: PlatformWalletDTO? = null,
    val redistributionWallet: PlatformWalletDTO? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AdminWalletsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AdminWalletsUiState())
    val uiState: StateFlow<AdminWalletsUiState> = _uiState.asStateFlow()

    init {
        loadWallets()
    }

    fun loadWallets() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val benefitsDeferred       = async { ForkEatApi.adminService.getBenefitsWallet() }
                val redistributionDeferred = async { ForkEatApi.adminService.getRedistributionWallet() }

                val benefits       = benefitsDeferred.await()
                val redistribution = redistributionDeferred.await()

                _uiState.value = _uiState.value.copy(
                    benefitsWallet       = if (benefits.isSuccessful) benefits.body() else null,
                    redistributionWallet = if (redistribution.isSuccessful) redistribution.body() else null,
                    isLoading            = false
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
