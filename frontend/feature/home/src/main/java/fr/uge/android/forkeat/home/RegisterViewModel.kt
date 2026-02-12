package fr.uge.android.forkeat.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.network.TokenManager
import fr.uge.android.forkeat.network.dto.LoginRequest
import fr.uge.android.forkeat.network.dto.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RegisterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
)

class RegisterViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState
    fun register(firstname: String, lastname: String, username: String, email: String, password: String, confirmPassword: String) {
        if (firstname.isBlank() || lastname.isBlank() || email.isBlank() || username.isBlank() || password.isBlank()) {
            _uiState.value = RegisterUiState(errorMessage = "Veuillez remplir tous les champs")
            return
        }

        if(password != confirmPassword){
            _uiState.value = RegisterUiState(errorMessage = "Les mots de passe sont différents")
            return
        }

        _uiState.value = RegisterUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val response = ForkEatApi.authService.register(RegisterRequest(firstname, lastname, email, username, password))
                if (response.isSuccessful) {
                    _uiState.value = RegisterUiState(isSuccess = true)
                } else {
                    _uiState.value = RegisterUiState(
                        errorMessage = "Identifiant et/ou mot de passe incorrect"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = RegisterUiState(
                    errorMessage = "Erreur de connexion au serveur"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}