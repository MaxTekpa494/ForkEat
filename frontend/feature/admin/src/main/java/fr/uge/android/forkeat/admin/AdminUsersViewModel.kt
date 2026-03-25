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
    val error: String? = null,
    val searchQuery: String = "",
    val userToPromote: UserResource? = null,
    val promoteSuccess: String? = null
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
                val query = _uiState.value.searchQuery
                val membersDeferred    = async { adminService.getMembers(query) }
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

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadUsers()
    }

    fun requestPromote(user: UserResource) {
        _uiState.value = _uiState.value.copy(userToPromote = user)
    }

    fun cancelPromote() {
        _uiState.value = _uiState.value.copy(userToPromote = null)
    }

    fun confirmPromote() {
        val user = _uiState.value.userToPromote ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(userToPromote = null, isLoading = true, error = null)
            try {
                val response = adminService.promoteToModerator(user.username)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        promoteSuccess = "${user.firstName} ${user.lastName} est maintenant modérateur",
                        isLoading = false
                    )
                    loadUsers()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erreur lors de la promotion (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erreur réseau : ${e.message}"
                )
            }
        }
    }

    fun clearPromoteSuccess() {
        _uiState.value = _uiState.value.copy(promoteSuccess = null)
    }
}
