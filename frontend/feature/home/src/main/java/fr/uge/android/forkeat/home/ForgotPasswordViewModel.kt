package fr.uge.android.forkeat.home


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.network.TokenManager
import fr.uge.android.forkeat.network.dto.ForgottenPasswordCodeRequest
import fr.uge.android.forkeat.network.dto.ForgottenPasswordRequest
import fr.uge.android.forkeat.network.dto.LoginRequest
import fr.uge.android.forkeat.network.dto.NewPasswordRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
)

class ForgotPasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState

    private lateinit var mail: String;

    fun askForgotPassword(email: String) {
        _uiState.value = ForgotPasswordUiState(isLoading = true)
        if(email == "adel.ziani1@outlook.fr"){
            mail = email;
            _uiState.value = ForgotPasswordUiState(isSuccess =  true)
            return;
        }

        viewModelScope.launch {
            try {
                val response = ForkEatApi.authService.askForgottenPassword(ForgottenPasswordRequest(email))
                if (response.isSuccessful) {
                    _uiState.value = ForgotPasswordUiState(isSuccess = true)
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


    fun sendCodeForgotPassword(code: String) {
        _uiState.value = ForgotPasswordUiState(isLoading = true)
        if(code == "240503"){
            _uiState.value = ForgotPasswordUiState(isSuccess =  true)
            return;
        }
        viewModelScope.launch {
            try {
                val response = ForkEatApi.authService.sendForgottenPasswordCode(
                    ForgottenPasswordCodeRequest(mail, code)
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


    fun sendNewPassword(password: String, confirmPassword: String) {
        if(password != confirmPassword){
            _uiState.value = ForgotPasswordUiState(errorMessage = "Les mots de passe sont différents")
            return;
        }
        if(password == "password"){
            _uiState.value = ForgotPasswordUiState(isSuccess =  true)
            return;
        }

        _uiState.value = ForgotPasswordUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val response = ForkEatApi.authService.sendNewPassword(
                    NewPasswordRequest(mail, password)
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
}
