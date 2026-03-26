package fr.uge.android.forkeat.home


import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.network.dto.ForgottenPasswordCodeRequest
import fr.uge.android.forkeat.network.dto.ForgottenPasswordRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val requiresCode: Boolean = true
)

class ForgotPasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState


    fun askForgotPassword(email: String) {
        _uiState.value = ForgotPasswordUiState(isLoading = true)

        viewModelScope.launch {
            try {

                MailForPasswordForgot.email = email
                val response = ForkEatApi.authService.askForgottenPassword(ForgottenPasswordRequest(email))
                if (response.isSuccessful) {
                    val statusDto = response.body()
                    if (statusDto != null) {
                        Log.d("ForgotPasswordViewModel", "Response: $statusDto")
                        _uiState.value = ForgotPasswordUiState(
                            isSuccess = true,
                            requiresCode = statusDto.resource.requiresCode
                        )
                    } else {
                        _uiState.value = ForgotPasswordUiState(
                            errorMessage = "Erreur lors du traitement de la réponse"
                        )
                    }
                } else {
                    _uiState.value = ForgotPasswordUiState(
                        errorMessage = "Email inexistant"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ForgotPasswordUiState(
                    errorMessage = "Erreur de connexion au serveur"
                )
            }
        }
    }


    fun sendCodeForgotPassword(code: String, password: String, confirmPassword: String) {
        _uiState.value = ForgotPasswordUiState(isLoading = true)
        if (MailForPasswordForgot.email == null) {
            _uiState.value = ForgotPasswordUiState(
                errorMessage = "Erreur Veuillez Réessayer"
            )
            return
        }
        viewModelScope.launch {
            try {
                val response = ForkEatApi.authService.sendForgottenPasswordCode(
                    ForgottenPasswordCodeRequest(
                        MailForPasswordForgot.email.toString(),
                        code,
                        password,
                        confirmPassword
                    )
                )
                if (response.isSuccessful) {
                    _uiState.value = ForgotPasswordUiState(isSuccess = true)
                } else {
                    _uiState.value = ForgotPasswordUiState(
                        errorMessage = "code incorrect"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ForgotPasswordUiState(
                    errorMessage = "Erreur de connexion au serveur"
                )
            }
        }
    }
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }
}
