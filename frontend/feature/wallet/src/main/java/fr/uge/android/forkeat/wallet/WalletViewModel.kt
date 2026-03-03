package fr.uge.android.forkeat.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.network.dto.wallet.BankInfoResponse
import fr.uge.android.forkeat.network.dto.wallet.CreateBankInfoRequest
import fr.uge.android.forkeat.network.dto.wallet.TopUpRequest
import fr.uge.android.forkeat.network.dto.wallet.TransactionDTO
import fr.uge.android.forkeat.network.dto.wallet.WithdrawRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WalletUiState(
    val balance: Long = 0,
    val transactions: List<TransactionDTO> = emptyList(),
    val bankInfo: BankInfoResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showBankInfoForm: Boolean = false,
    val showRechargeDialog: Boolean = false,
    val showWithdrawDialog: Boolean = false,
    val rechargeUrl: String? = null,
    val successMessage: String? = null
)

data class BalanceUIState(
    val balance: Long = 0,
    val isLoading: Boolean = false,
    val error: String? = null
    )

class WalletViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val _balanceUiState = MutableStateFlow(BalanceUIState())
    val balanceUiState: StateFlow<BalanceUIState> = _balanceUiState.asStateFlow()

    private val walletApi = ForkEatApi.walletService

    fun loadWallet() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val balanceDeferred = async { walletApi.getBalance() }
                val transactionsDeferred = async { walletApi.getTransactions() }
                val bankInfoDeferred = async { walletApi.getBankInfo() }

                val balanceResponse = balanceDeferred.await()
                val transactionsResponse = transactionsDeferred.await()
                val bankInfoResponse = bankInfoDeferred.await()

                _uiState.update { state ->
                    state.copy(
                        balance = if (balanceResponse.isSuccessful) balanceResponse.body()?.balance ?: 0 else 0,
                        transactions = if (transactionsResponse.isSuccessful) transactionsResponse.body() ?: emptyList() else emptyList(),
                        bankInfo = if (bankInfoResponse.isSuccessful) bankInfoResponse.body() else null,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erreur de chargement: ${e.message}") }
            }
        }
    }

    fun loadBalance(){
        viewModelScope.launch {
            _balanceUiState.update { it.copy(isLoading = true, error = null) }
            try {
                val balanceDeferred = async { walletApi.getBalance() }

                val balanceResponse = balanceDeferred.await()

                _balanceUiState.update { state ->
                    state.copy(
                        balance = if (balanceResponse.isSuccessful) balanceResponse.body()?.balance ?: 0 else 0,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _balanceUiState.update { it.copy(isLoading = false, error = "Erreur de chargement: ${e.message}") }
            }
        }
    }

    fun saveBankInfo(bankName: String, iban: String, bic: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = walletApi.saveBankInfo(
                    CreateBankInfoRequest(bankName = bankName, iban = iban, bic = bic)
                )
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            bankInfo = response.body(),
                            showBankInfoForm = false,
                            isLoading = false,
                            successMessage = "Compte bancaire enregistre"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Erreur lors de l'enregistrement du compte bancaire") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erreur: ${e.message}") }
            }
        }
    }

    fun recharge(amountEuros: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = walletApi.recharge(TopUpRequest(amount = amountEuros * 100))
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            rechargeUrl = response.body()?.url,
                            showRechargeDialog = false,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Erreur lors de la recharge") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erreur: ${e.message}") }
            }
        }
    }

    fun withdraw(amountEuros: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = walletApi.withdraw(WithdrawRequest(amount = amountEuros * 100))
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            showWithdrawDialog = false,
                            isLoading = false,
                            successMessage = "Retrait initie avec succes"
                        )
                    }
                    loadWallet()
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Erreur lors du retrait") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erreur: ${e.message}") }
            }
        }
    }

    fun onRechargeUrlConsumed() {
        _uiState.update { it.copy(rechargeUrl = null) }
    }

    fun onSuccessMessageConsumed() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun showRechargeDialog() {
        _uiState.update { it.copy(showRechargeDialog = true) }
    }

    fun dismissRechargeDialog() {
        _uiState.update { it.copy(showRechargeDialog = false) }
    }

    fun showWithdrawDialog() {
        _uiState.update { it.copy(showWithdrawDialog = true) }
    }

    fun dismissWithdrawDialog() {
        _uiState.update { it.copy(showWithdrawDialog = false) }
    }

    fun showBankInfoForm() {
        _uiState.update { it.copy(showBankInfoForm = true) }
    }

    fun dismissBankInfoForm() {
        _uiState.update { it.copy(showBankInfoForm = false) }
    }
}
