package fr.uge.android.forkeat.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.network.dto.ConfirmPasswordChangeRequest
import fr.uge.android.forkeat.network.dto.PasswordChangeRequest
import fr.uge.android.forkeat.network.dto.SetPasswordRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccountUiState(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val email: String = "",
    val role: String = "MEMBER",
    val authMode: String = "",
    val isEditingInfo: Boolean = false,
    val showEmailModal: Boolean = false,
    val showPasswordModal: Boolean = false,
    val showSetPasswordModal: Boolean = false,
    val showConfirmPasswordModal: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AccountViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

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

    // Conserve le nouveau mot de passe entre request et confirm
    private var _pendingNewPassword = ""
    private var _pendingConfirmPassword = ""

    init {
        loadAccount()
    }

    fun loadAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = ForkEatApi.accountService.getAccount()
                if (response.isSuccessful) {
                    val user = response.body()?.resource
                    if (user != null) {
                        _uiState.value = _uiState.value.copy(
                            firstName = user.firstName,
                            lastName = user.lastName,
                            username = user.username,
                            email = user.email,
                            role = user.role,
                            authMode = user.authMode,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erreur de chargement du compte"
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = ForkEatApi.accountService.requestPasswordChange(
                    PasswordChangeRequest(
                        currentPassword = _currentPassword.value,
                        newPassword = _newPassword.value,
                        confirmPassword = _confirmNewPassword.value
                    )
                )
                if (response.isSuccessful) {
                    _pendingNewPassword = _newPassword.value
                    _pendingConfirmPassword = _confirmNewPassword.value
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showPasswordModal = false,
                        showConfirmPasswordModal = true,
                        error = null
                    )
                    _currentPassword.value = ""
                    _newPassword.value = ""
                    _confirmNewPassword.value = ""
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Mot de passe actuel incorrect."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Erreur: ${e.message}")
            }
        }
    }

    fun confirmPasswordChange(code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = ForkEatApi.accountService.confirmPasswordChange(
                    ConfirmPasswordChangeRequest(
                        code = code,
                        newPassword = _pendingNewPassword,
                        confirmPassword = _pendingConfirmPassword
                    )
                )
                if (response.isSuccessful) {
                    _pendingNewPassword = ""
                    _pendingConfirmPassword = ""
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showConfirmPasswordModal = false,
                        error = null
                    )
                    loadAccount()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Code invalide ou expiré."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Erreur: ${e.message}")
            }
        }
    }

    fun closeConfirmPasswordModal() {
        _pendingNewPassword = ""
        _pendingConfirmPassword = ""
        _uiState.value = _uiState.value.copy(showConfirmPasswordModal = false, error = null)
    }

    fun openSetPasswordModal() {
        _uiState.value = _uiState.value.copy(showSetPasswordModal = true, error = null)
    }

    fun closeSetPasswordModal() {
        _uiState.value = _uiState.value.copy(showSetPasswordModal = false, error = null)
        _newPassword.value = ""
        _confirmNewPassword.value = ""
    }

    fun setPassword() {
        if (_newPassword.value != _confirmNewPassword.value) {
            _uiState.value = _uiState.value.copy(error = "Les mots de passe ne correspondent pas.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = ForkEatApi.accountService.setPassword(
                    SetPasswordRequest(
                        newPassword = _newPassword.value,
                        confirmPassword = _confirmNewPassword.value
                    )
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showSetPasswordModal = false,
                        error = null
                    )
                    loadAccount()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Le mot de passe doit contenir au moins 8 caractères."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Erreur: ${e.message}")
            } finally {
                _newPassword.value = ""
                _confirmNewPassword.value = ""
            }
        }
    }

    fun deleteAccount() {
        // TODO: Implement logic to delete account
    }
}
