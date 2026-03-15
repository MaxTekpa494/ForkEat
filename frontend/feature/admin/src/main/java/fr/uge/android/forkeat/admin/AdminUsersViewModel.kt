package fr.uge.android.forkeat.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.network.dto.UserResource
import fr.uge.android.forkeat.admin.data.api.AdminApi

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUsersUiState(
    val members: List<UserResource> = emptyList(),
    val moderators: List<UserResource> = emptyList(),
    val admins: List<UserResource> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AdminUsersViewModel : ViewModel() {

    private val adminService = AdminApi.service

    private val _uiState = MutableStateFlow(AdminUsersUiState())
    val uiState: StateFlow<AdminUsersUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val membersDeferred    = async { adminService.getMembers() }
                val moderatorsDeferred = async { adminService.getModerators() }
                val adminsDeferred     = async { adminService.getAdmins() }

                val membersResponse    = membersDeferred.await()
                val moderatorsResponse = moderatorsDeferred.await()
                val adminsResponse     = adminsDeferred.await()

                val currentUsername = ForkEatApi.getCurrentUsername()

                _uiState.value = _uiState.value.copy(
                    members    = if (membersResponse.isSuccessful)
                        membersResponse.body()?.items ?: emptyList()
                    else emptyList(),
                    moderators = if (moderatorsResponse.isSuccessful)
                        moderatorsResponse.body()?.items ?: emptyList()
                    else emptyList(),
                    admins     = if (adminsResponse.isSuccessful)
                        (adminsResponse.body()?.items ?: emptyList())
                            .filter { it.username != currentUsername }
                    else emptyList(),
                    isLoading  = false
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
