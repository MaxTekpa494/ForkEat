package fr.uge.android.forkeat.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.network.dto.admin.AdminCreateUserRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AdminCreateRole { MODERATOR, ADMIN }

data class AdminCreateUserUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

class AdminCreateUserViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AdminCreateUserUiState())
    val uiState: StateFlow<AdminCreateUserUiState> = _uiState.asStateFlow()

    fun createUser(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        role: AdminCreateRole
    ) {
        if (username.isBlank() || firstName.isBlank() || lastName.isBlank() ||
            email.isBlank() || password.isBlank()
        ) {
            _uiState.value = _uiState.value.copy(error = "Veuillez remplir tous les champs")
            return
        }
        if (password.length < 8) {
            _uiState.value = _uiState.value.copy(error = "Le mot de passe doit contenir au moins 8 caractères")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, isSuccess = false)
            try {
                val request = AdminCreateUserRequest(username, firstName, lastName, email, password)
                val response = when (role) {
                    AdminCreateRole.MODERATOR -> ForkEatApi.adminService.createModerator(request)
                    AdminCreateRole.ADMIN     -> ForkEatApi.adminService.createAdmin(request)
                }
                if (response.isSuccessful) {
                    val roleLabel = if (role == AdminCreateRole.MODERATOR) "modérateur" else "administrateur"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        successMessage = "Le compte $roleLabel « $username » a été créé avec succès."
                    )
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = when {
                            errorBody.contains("email", ignoreCase = true) -> "Cet email est déjà utilisé"
                            errorBody.contains("username", ignoreCase = true) -> "Ce nom d'utilisateur est déjà pris"
                            else -> "Erreur lors de la création (${response.code()})"
                        }
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

    fun clearStatus() {
        _uiState.value = _uiState.value.copy(error = null, isSuccess = false, successMessage = null)
    }
}
