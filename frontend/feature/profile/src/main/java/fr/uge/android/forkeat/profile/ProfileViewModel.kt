package fr.uge.android.forkeat.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class ProfileUiState(
    val firstName: String = "John",
    val lastName: String = "Doe",
    val username: String = "johndoe",
    val email: String = "john.doe@example.com",
    val memberSince: LocalDate = LocalDate.of(2023, 1, 1),
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
        // TODO: Fetch initial profile data
        // For now, using mock data
        _uiState.value = ProfileUiState(
            firstName = "Jean",
            lastName = "Dupont",
            username = "jeandupont",
            email = "jean@exemple.com",
            memberSince = LocalDate.of(2023, 5, 15),
            role = "MEMBER",
            totalRecipes = 5,
            totalLikes = 150,
            followers = 75,
            superLikes = 10
        )
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
        _uiState.value = _uiState.value.copy(isLoading = true)
        // Simulate network call
        // delay(1000)
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isEditingInfo = false,
            error = null // Clear any previous errors
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
        _uiState.value = _uiState.value.copy(isLoading = true)
        // Simulate network call
        // delay(1000)
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            showEmailModal = false,
            error = null
        )
        // Update local email if successful
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
        // TODO: Implement logic to update password to backend
        if (_newPassword.value != _confirmNewPassword.value) {
            _uiState.value = _uiState.value.copy(error = "Les nouveaux mots de passe ne correspondent pas.")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        // Simulate network call
        // delay(1000)
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
        // TODO: Handle navigation to my recipes screen
    }

    fun navigateToDashboard() {
        // TODO: Handle navigation to dashboard screen
    }

    fun navigateToWallet() {
        // TODO: Handle navigation to wallet screen
    }

    fun navigateToCreateRecipe() {
        // TODO: Handle navigation to create recipe screen
    }
}
