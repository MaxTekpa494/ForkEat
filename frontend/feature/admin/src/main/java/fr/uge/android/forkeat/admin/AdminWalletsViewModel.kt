package fr.uge.android.forkeat.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.admin.data.api.AdminApi
import fr.uge.android.forkeat.admin.data.dto.PlatformWalletDTO
import fr.uge.android.forkeat.admin.data.dto.PlatformWalletTransactionDTO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminWalletsUiState(
    val benefitsWallet: PlatformWalletDTO? = null,
    val redistributionWallet: PlatformWalletDTO? = null,
    val transactions: List<PlatformWalletTransactionDTO> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AdminWalletsViewModel : ViewModel() {

    private val adminService = AdminApi.service

    private val _uiState = MutableStateFlow(AdminWalletsUiState())
    val uiState: StateFlow<AdminWalletsUiState> = _uiState.asStateFlow()

    init {
        loadWallets()
    }

    fun loadWallets() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val benefitsDeferred       = async { adminService.getBenefitsWallet() }
                val redistributionDeferred = async { adminService.getRedistributionWallet() }
                val transactionsDeferred   = async { adminService.getWalletTransactions() }

                val benefits       = benefitsDeferred.await()
                val redistribution = redistributionDeferred.await()
                val transactions   = transactionsDeferred.await()

                _uiState.value = _uiState.value.copy(
                    benefitsWallet       = if (benefits.isSuccessful) benefits.body() else null,
                    redistributionWallet = if (redistribution.isSuccessful) redistribution.body() else null,
                    transactions         = if (transactions.isSuccessful) transactions.body() ?: emptyList() else emptyList(),
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
