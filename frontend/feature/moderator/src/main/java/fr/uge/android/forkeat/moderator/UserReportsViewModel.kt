package fr.uge.android.forkeat.moderator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.moderator.data.api.ModeratorApi
import fr.uge.android.forkeat.moderator.data.dto.UserModerationRequest
import fr.uge.android.forkeat.moderator.data.dto.UserReportDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class UserReportsUiState(
    val reports: List<UserReportDetails> = emptyList(),
    val currentPage: Int = 0,
    val total: Int = 0,
    val isLoading: Boolean = false,
    val actionInProgress: UUID? = null,
    val error: String? = null,
    val successMessage: String? = null
)

class UserReportsViewModel : ViewModel() {
    private val moderatorService = ModeratorApi.service

    private val _uiState = MutableStateFlow(UserReportsUiState())
    val uiState: StateFlow<UserReportsUiState> = _uiState.asStateFlow()

    init {
        loadReports()
    }

    fun loadReports(page: Int = 0, size: Int = 10, append: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = moderatorService.getReportedUsers(size, page)
                if (response.isSuccessful) {
                    val body = response.body()
                    val current = if (append) _uiState.value.reports else emptyList()
                    val allReports = current + (body?.resources ?: emptyList())
                    _uiState.value = _uiState.value.copy(
                        reports = allReports,
                        total = body?.total ?: 0,
                        currentPage = page,
                        isLoading = false,
                        error = null
                    )
                } else {
                    val code = response.code()
                    val error = when {
                        code >= 500 -> "Le serveur est indisponible, veuillez réessayer plus tard."
                        code in 400..499 -> "Une erreur est survenue, veuillez réessayer."
                        else -> "Une erreur inconnue est survenue."
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error
                    )
                }
            } catch (_: Exception) {
                val error = "Le serveur est indisponible, veuillez réessayer plus tard."
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error
                )
            }
        }
    }

    fun loadMoreReports() {
        val uiState = _uiState.value
        if (uiState.isLoading || uiState.reports.size >= uiState.total) return
        val nextPage = uiState.currentPage + 1
        loadReports(nextPage, append = true)
    }

    fun resolveUserReport(reportId: UUID, request: UserModerationRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = reportId, error = null)
            try {
                val response = moderatorService.resolveUserReport(reportId, request)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = null,
                        reports = _uiState.value.reports.filter { it.id != reportId },
                        successMessage = "Signalement utilisateur résolu"
                    )
                    loadReports()
                } else {
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = null,
                        error = "Erreur lors de la résolution : ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    actionInProgress = null,
                    error = "Erreur : ${e.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null, error = null)
    }
}
