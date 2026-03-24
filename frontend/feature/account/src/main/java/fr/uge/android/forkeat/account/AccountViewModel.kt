package fr.uge.android.forkeat.account

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.profile.data.api.AccountApi
import fr.uge.android.forkeat.profile.data.dto.PasswordChangeRequest
import fr.uge.android.forkeat.profile.data.dto.RequestEmailChangeRequest
import fr.uge.android.forkeat.profile.data.dto.SetPasswordRequest
import fr.uge.android.forkeat.profile.data.dto.UpdateProfileRequest
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
    val emailVerified: Boolean = false,
    val isEditingInfo: Boolean = false,
    val showEmailModal: Boolean = false,
    val showConfirmEmailModal: Boolean = false,
    val showPasswordModal: Boolean = false,
    val showSetPasswordModal: Boolean = false,
    val showConfirmPasswordModal: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val resendMessage: String? = null,
    val showDeleteModal: Boolean = false
)

class AccountViewModel : ViewModel() {

    private val accountService = AccountApi.service

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

    // Conserve les valeurs originales pour annuler l'édition du profil
    private var _originalFirstName = ""
    private var _originalLastName = ""
    private var _originalUsername = ""

    init {
        loadAccount()
    }

    fun loadAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = accountService.getAccount()
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
                            emailVerified = user.emailVerified,
                            isLoading = false
                        )
                    } else {
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
        if (!_uiState.value.isEditingInfo) {
            _originalFirstName = _uiState.value.firstName
            _originalLastName = _uiState.value.lastName
            _originalUsername = _uiState.value.username
            _uiState.value = _uiState.value.copy(isEditingInfo = true, error = null)
        } else {
            _uiState.value = _uiState.value.copy(
                isEditingInfo = false,
                firstName = _originalFirstName,
                lastName = _originalLastName,
                username = _originalUsername,
                error = null
            )
        }
    }

    fun saveProfileInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = accountService.updateProfile(
                    UpdateProfileRequest(
                        firstName = _uiState.value.firstName,
                        lastName = _uiState.value.lastName,
                        username = _uiState.value.username
                    )
                )
                if (response.isSuccessful) {
                    val user = response.body()?.resource
                    if (user != null) {
                        _uiState.value = _uiState.value.copy(
                            firstName = user.firstName,
                            lastName = user.lastName,
                            username = user.username,
                            isLoading = false,
                            isEditingInfo = false,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, isEditingInfo = false)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Ce nom d'utilisateur est déjà pris."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Erreur: ${e.message}")
            }
        }
    }

    fun openEmailModal() {
        _uiState.value = _uiState.value.copy(showEmailModal = true, error = null)
    }

    fun closeEmailModal() {
        _uiState.value = _uiState.value.copy(showEmailModal = false, error = null)
        _newEmail.value = ""
        _currentPasswordEmailConfirm.value = ""
        _newPassword.value = ""
        _confirmNewPassword.value = ""
    }

    fun onNewEmailChange(newValue: String) {
        _newEmail.value = newValue
    }

    fun onCurrentPasswordEmailConfirmChange(newValue: String) {
        _currentPasswordEmailConfirm.value = newValue
    }

    fun updateEmail() {
        val isGoogleUser = _uiState.value.authMode == "GOOGLE"
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = accountService.requestEmailChange(
                    if (isGoogleUser) {
                        RequestEmailChangeRequest(
                            newEmail = _newEmail.value,
                            currentPassword = "",
                            newPassword = _newPassword.value,
                            confirmPassword = _confirmNewPassword.value
                        )
                    } else {
                        RequestEmailChangeRequest(
                            newEmail = _newEmail.value,
                            currentPassword = _currentPasswordEmailConfirm.value,
                            newPassword = "",
                            confirmPassword = ""
                        )
                    }
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showEmailModal = false,
                        showConfirmEmailModal = true,
                        error = null
                    )
                    _currentPasswordEmailConfirm.value = ""
                    _newPassword.value = ""
                    _confirmNewPassword.value = ""
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = if (isGoogleUser) "Mot de passe invalide ou email déjà utilisé." else "Mot de passe incorrect ou email déjà utilisé."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Erreur: ${e.message}")
            }
        }
    }

    fun confirmEmailChange(code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = accountService.confirmEmailChange(code)
                if (response.isSuccessful) {
                    val user = response.body()?.resource
                    if (user != null) {
                        _uiState.value = _uiState.value.copy(
                            email = user.email,
                            emailVerified = user.emailVerified,
                            isLoading = false,
                            showConfirmEmailModal = false,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, showConfirmEmailModal = false)
                    }
                    _newEmail.value = ""
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

    fun closeConfirmEmailModal() {
        _newEmail.value = ""
        _uiState.value = _uiState.value.copy(showConfirmEmailModal = false, error = null)
    }

    fun openPasswordModal() {
        _uiState.value = _uiState.value.copy(showPasswordModal = true, error = null)
    }

    fun closePasswordModal() {
        _uiState.value = _uiState.value.copy(showPasswordModal = false, error = null)
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
                val response = accountService.requestPasswordChange(
                    PasswordChangeRequest(
                        currentPassword = _currentPassword.value,
                        newPassword = _newPassword.value,
                        confirmPassword = _confirmNewPassword.value
                    )
                )
                if (response.isSuccessful) {
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
                val response = accountService.confirmPasswordChange(code)
                if (response.isSuccessful) {
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
                val response = accountService.setPassword(
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

    fun resendConfirmation() {
        viewModelScope.launch {
            try {
                val response = accountService.resendConfirmation()
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(resendMessage = "Email de confirmation envoyé !")
                } else {
                    _uiState.value = _uiState.value.copy(resendMessage = "Impossible d'envoyer l'email. Veuillez réessayer.")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(resendMessage = "Erreur: ${e.message}")
            }
        }
    }

    fun dismissResendMessage() {
        _uiState.value = _uiState.value.copy(resendMessage = null)
    }

    fun openDeleteModal() {
        _uiState.value = _uiState.value.copy(showDeleteModal = true, error = null)
    }

    fun closeDeleteModal() {
        _uiState.value = _uiState.value.copy(showDeleteModal = false, error = null)
    }

    fun deleteAccount(onLogout: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = accountService.deleteAccount()
                if (response.isSuccessful) {
                    onLogout()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showDeleteModal = false,
                        error = "Erreur lors de la suppression du compte."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showDeleteModal = false,
                    error = "Erreur: ${e.message}"
                )
            }
        }
    }
}