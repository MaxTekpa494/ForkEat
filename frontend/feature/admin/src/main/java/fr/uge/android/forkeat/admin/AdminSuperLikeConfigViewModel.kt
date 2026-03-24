package fr.uge.android.forkeat.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.admin.data.api.AdminApi
import fr.uge.android.forkeat.admin.data.dto.SuperLikeConfigDTO
import fr.uge.android.forkeat.admin.data.dto.UpdateSuperLikeConfigRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminSuperLikeConfigUiState(
    val config: SuperLikeConfigDTO? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

class AdminSuperLikeConfigViewModel : ViewModel() {

    private val api = AdminApi.service

    private val _uiState = MutableStateFlow(AdminSuperLikeConfigUiState())
    val uiState: StateFlow<AdminSuperLikeConfigUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = api.getSuperLikeConfig()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    config = if (response.isSuccessful) response.body()?.resource else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun save(priceCents: Long, earningsRatio: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val response = api.updateSuperLikeConfig(UpdateSuperLikeConfigRequest(priceCents, earningsRatio))
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(isSaving = false, success = true, config = response.body()?.resource)
                } else {
                    _uiState.value = _uiState.value.copy(isSaving = false, error = "Erreur de sauvegarde")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    fun dismissSuccess() { _uiState.value = _uiState.value.copy(success = false) }
    fun dismissError() { _uiState.value = _uiState.value.copy(error = null) }
}
