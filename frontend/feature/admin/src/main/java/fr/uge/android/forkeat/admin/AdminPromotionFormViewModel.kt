package fr.uge.android.forkeat.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.admin.data.api.AdminApi
import fr.uge.android.forkeat.admin.data.dto.CreatePromotionRequest
import fr.uge.android.forkeat.admin.data.dto.SuperLikeConfigDTO
import fr.uge.android.forkeat.admin.data.dto.UpdatePromotionRequest
import fr.uge.android.forkeat.promotions.data.dto.PromotionDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class AdminPromotionFormUiState(
    val promotion: PromotionDTO? = null,   // null = création
    val config: SuperLikeConfigDTO? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

class AdminPromotionFormViewModel : ViewModel() {

    private val api = AdminApi.service

    private val _uiState = MutableStateFlow(AdminPromotionFormUiState())
    val uiState: StateFlow<AdminPromotionFormUiState> = _uiState.asStateFlow()

    fun load(promotionId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val configResponse = api.getSuperLikeConfig()
                val config = if (configResponse.isSuccessful) configResponse.body()?.resource else null

                val promotion = if (promotionId != null) {
                    val r = api.getPromotion(promotionId)
                    if (r.isSuccessful) r.body()?.resource else null
                } else null

                _uiState.value = _uiState.value.copy(isLoading = false, promotion = promotion, config = config)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun create(name: String, startsAt: String, endsAt: String, priceCents: Long, bonusEveryN: Int?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val response = api.createPromotion(CreatePromotionRequest(name, startsAt, endsAt, priceCents, bonusEveryN))
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(isSaving = false, success = true)
                } else {
                    _uiState.value = _uiState.value.copy(isSaving = false, error = parseApiError(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    fun update(id: String, name: String, startsAt: String, endsAt: String, priceCents: Long, bonusEveryN: Int?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val response = api.updatePromotion(id, UpdatePromotionRequest(name, startsAt, endsAt, priceCents, bonusEveryN))
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(isSaving = false, success = true)
                } else {
                    _uiState.value = _uiState.value.copy(isSaving = false, error = parseApiError(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    private fun parseApiError(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "Erreur inconnue"
        return try {
            JSONObject(errorBody).getString("message")
        } catch (_: Exception) {
            "Erreur inconnue"
        }
    }

    fun dismissError() { _uiState.value = _uiState.value.copy(error = null) }
}
