package fr.uge.android.forkeat.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

import java.time.LocalDate

sealed class ProfileNavigationEvent {
    data object NavigateToCreateRecipe : ProfileNavigationEvent()
    data object NavigateToMyRecipes : ProfileNavigationEvent()
}

data class ProfileUiState(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val email: String = "",
    val memberSince: LocalDate = LocalDate.now(),
    val role: String = "MEMBER",
    val totalRecipes: Int = 0,
    val totalLikes: Int = 0,
    val followers: Int = 0,
    val superLikes: Int = 0,
    val isEditingInfo: Boolean = false,
    val showEmailModal: Boolean = false,
    val showPasswordModal: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProfileViewModel : ViewModel() {
    private val _navigationEvent = Channel<ProfileNavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _newEmail = MutableStateFlow("")
    val newEmail: StateFlow<String> = _newEmail.asStateFlow()

    private val _currentPasswordEmailConfirm = MutableStateFlow("")
    val currentPasswordEmailConfirm: StateFlow<String> = _currentPasswordEmailConfirm.asStateFlow()

    private val _currentPassword = MutableStateFlow("")
    val currentPassword: StateFlow<String> = _currentPassword.asStateFlow()

    private val _newPassword = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword.asStateFlow()

    private val _confirmNewPassword = MutableStateFlow("")
    val confirmNewPassword: StateFlow<String> = _confirmNewPassword.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = ForkEatApi.authService.me()
                if (response.isSuccessful) {
                    val user = response.body()?.resource
                    if (user != null) {
                        _uiState.value = _uiState.value.copy(
                            firstName = user.firstName,
                            lastName = user.lastName,
                            username = user.username,
                            email = user.email,
                            role = user.role,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erreur de chargement du profil"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erreur: ${e.message}"
                )
            }
        }
    }

    fun onFirstNameChange(newValue: String) {
        _uiState.value = _uiState.value.copy(firstName = newValue)
    }

    fun onLastNameChange(newValue: String) {
        _uiState.value = _uiState.value.copy(lastName = newValue)
    }

    fun onUsernameChange(newValue: String) {
        _uiState.value = _uiState.value.copy(username = newValue)
    }

    fun toggleEditInfo() {
        _uiState.value = _uiState.value.copy(isEditingInfo = !_uiState.value.isEditingInfo)
    }

    fun saveProfileInfo() {
        // TODO: Implement logic to save profile info to backend
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isEditingInfo = false,
            error = null
        )
    }

    fun openEmailModal() {
        _uiState.value = _uiState.value.copy(showEmailModal = true)
    }

    fun closeEmailModal() {
        _uiState.value = _uiState.value.copy(showEmailModal = false)
        _newEmail.value = ""
        _currentPasswordEmailConfirm.value = ""
    }

    fun onNewEmailChange(newValue: String) {
        _newEmail.value = newValue
    }

    fun onCurrentPasswordEmailConfirmChange(newValue: String) {
        _currentPasswordEmailConfirm.value = newValue
    }

    fun updateEmail() {
        // TODO: Implement logic to update email to backend
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            showEmailModal = false,
            error = null
        )
        if (_uiState.value.error == null) {
            _uiState.value = _uiState.value.copy(email = _newEmail.value)
        }
        _newEmail.value = ""
        _currentPasswordEmailConfirm.value = ""
    }

    fun openPasswordModal() {
        _uiState.value = _uiState.value.copy(showPasswordModal = true)
    }

    fun closePasswordModal() {
        _uiState.value = _uiState.value.copy(showPasswordModal = false)
        _currentPassword.value = ""
        _newPassword.value = ""
        _confirmNewPassword.value = ""
    }

    fun onCurrentPasswordChange(newValue: String) {
        _currentPassword.value = newValue
    }

    fun onNewPasswordChange(newValue: String) {
        _newPassword.value = newValue
    }

    fun onConfirmNewPasswordChange(newValue: String) {
        _confirmNewPassword.value = newValue
    }

    fun updatePassword() {
        if (_newPassword.value != _confirmNewPassword.value) {
            _uiState.value = _uiState.value.copy(error = "Les nouveaux mots de passe ne correspondent pas.")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            showPasswordModal = false,
            error = null
        )
        _currentPassword.value = ""
        _newPassword.value = ""
        _confirmNewPassword.value = ""
    }

    fun activateTwoFactorAuth() {
        // TODO: Implement logic to activate 2FA
    }

    fun deleteAccount() {
        // TODO: Implement logic to delete account
    }

    fun navigateToMyRecipes() {
        viewModelScope.launch {
            _navigationEvent.send(ProfileNavigationEvent.NavigateToMyRecipes)
        }
    }

    fun navigateToDashboard() {
        // TODO: Handle navigation to dashboard screen
    }

    fun navigateToWallet() {
        // TODO: Handle navigation to wallet screen
    }

    fun navigateToCreateRecipe() {
        viewModelScope.launch {
            _navigationEvent.send(ProfileNavigationEvent.NavigateToCreateRecipe)
        }
    }
}
