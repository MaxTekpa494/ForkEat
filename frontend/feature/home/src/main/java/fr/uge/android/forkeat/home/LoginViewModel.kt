package fr.uge.android.forkeat.home

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.network.TokenManager
import fr.uge.android.forkeat.network.dto.GoogleLoginRequest
import fr.uge.android.forkeat.network.dto.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState(errorMessage = "Veuillez remplir tous les champs")
            return
        }

        _uiState.value = LoginUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val response = ForkEatApi.authService.login(LoginRequest(username, password))
                if (response.isSuccessful && response.body() != null) {
                    val token = "Bearer " + response.body()!!.token
                    tokenManager.saveToken(token)
                    _uiState.value = LoginUiState(isSuccess = true)
                } else {
                    _uiState.value = LoginUiState(
                        errorMessage = "Email ou mot de passe incorrect"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState(
                    errorMessage = "Erreur de connexion au serveur"
                )
            }
        }
    }

    fun loginWithGoogle(context: Context) {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) {
            _uiState.value = LoginUiState(
                errorMessage = "Google Sign-In non configuré (GOOGLE_CLIENT_ID manquant)"
            )
            return
        }

        _uiState.value = LoginUiState(isLoading = true)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                val idToken = googleIdTokenCredential.idToken

                val response = ForkEatApi.authService.loginWithGoogle(GoogleLoginRequest(idToken))
                if (response.isSuccessful && response.body() != null) {
                    tokenManager.saveToken(response.body()!!.token)
                    _uiState.value = LoginUiState(isSuccess = true)
                } else {
                    _uiState.value = LoginUiState(
                        errorMessage = "Echec de l'authentification Google"
                    )
                }
            } catch (e: GetCredentialCancellationException) {
                _uiState.value = LoginUiState()
            } catch (e: NoCredentialException) {
                Log.e("LoginViewModel", "No Google account available", e)
                _uiState.value = LoginUiState(
                    errorMessage = "Aucun compte Google trouvé. Ajoutez un compte dans les paramètres."
                )
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Google sign-in failed: ${e::class.simpleName}", e)
                _uiState.value = LoginUiState(
                    errorMessage = "Erreur Google: ${e.message ?: e::class.simpleName}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
