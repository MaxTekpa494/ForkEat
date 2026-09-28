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
import fr.uge.android.forkeat.network.dto.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RegisterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val showEmailSentBanner: Boolean = false,
)

class RegisterViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    fun register(firstname: String, lastname: String, username: String, email: String, password: String, confirmPassword: String) {
        if (firstname.isBlank() || lastname.isBlank() || email.isBlank() || username.isBlank() || password.isBlank()) {
            _uiState.value = RegisterUiState(errorMessage = "Veuillez remplir tous les champs")
            return
        }

        if (password != confirmPassword) {
            _uiState.value = RegisterUiState(errorMessage = "Les mots de passe sont différents")
            return
        }

        _uiState.value = RegisterUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val response = ForkEatApi.authService.register(RegisterRequest(firstname, lastname, email, username, password))
                if (response.isSuccessful) {
                    _uiState.value = RegisterUiState(isSuccess = true, showEmailSentBanner = true)
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

    fun loginWithGoogle(context: Context) {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) {
            _uiState.value = RegisterUiState(
                errorMessage = "Google Sign-In non configuré (GOOGLE_CLIENT_ID manquant)"
            )
            return
        }

        _uiState.value = RegisterUiState(isLoading = true)

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
                    // On ne sauvegarde pas le token ici pour forcer l'utilisateur à se connecter
                    _uiState.value = RegisterUiState(isSuccess = true)
                } else {
                    _uiState.value = RegisterUiState(
                        errorMessage = "Echec de l'authentification Google"
                    )
                }
            } catch (e: GetCredentialCancellationException) {
                _uiState.value = RegisterUiState()
            } catch (e: NoCredentialException) {
                Log.e("RegisterViewModel", "No Google account available", e)
                _uiState.value = RegisterUiState(
                    errorMessage = "Aucun compte Google trouvé. Ajoutez un compte dans les paramètres."
                )
            } catch (e: Exception) {
                Log.e("RegisterViewModel", "Google sign-in failed: ${e::class.simpleName}", e)
                _uiState.value = RegisterUiState(
                    errorMessage = "Erreur Google: ${e.message ?: e::class.simpleName}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
