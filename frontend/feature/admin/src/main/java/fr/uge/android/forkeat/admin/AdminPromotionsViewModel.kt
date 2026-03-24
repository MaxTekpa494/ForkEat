package fr.uge.android.forkeat.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.admin.data.api.AdminApi
import fr.uge.android.forkeat.promotions.data.dto.PromotionDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminPromotionsUiState(
    val promotions: List<PromotionDTO> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val cancelSuccess: Boolean = false
)

class AdminPromotionsViewModel : ViewModel() {

    private val api = AdminApi.service

    private val _uiState = MutableStateFlow(AdminPromotionsUiState())
    val uiState: StateFlow<AdminPromotionsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = api.getAllPromotions()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    promotions = if (response.isSuccessful) response.body()?.resources ?: emptyList() else emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun cancel(id: String) {
        viewModelScope.launch {
            try {
                val response = api.cancelPromotion(id)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(cancelSuccess = true)
                    load()
                } else {
                    _uiState.value = _uiState.value.copy(error = "Impossible d'annuler la promotion")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun dismissCancelSuccess() { _uiState.value = _uiState.value.copy(cancelSuccess = false) }
    fun dismissError() { _uiState.value = _uiState.value.copy(error = null) }
}
