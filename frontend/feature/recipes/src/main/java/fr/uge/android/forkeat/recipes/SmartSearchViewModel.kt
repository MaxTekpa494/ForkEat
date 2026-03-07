package fr.uge.android.forkeat.recipes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.recipes.data.dto.PersonalizedRecipeSummaryDTO
import fr.uge.android.forkeat.recipes.data.dto.SmartSearchRequestDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SmartSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val recipeApi = ForkEatApi.recipeService
    private val walletApi = ForkEatApi.walletService

    // Session-like persistence : la requête et les résultats survivent à la navigation
    // tant que le ViewModel est vivant (durée de vie de l'Activity = session utilisateur)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<PersonalizedRecipeSummaryDTO>>(emptyList())
    val results: StateFlow<List<PersonalizedRecipeSummaryDTO>> = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isModerationError = MutableStateFlow(false)
    val isModerationError: StateFlow<Boolean> = _isModerationError.asStateFlow()

    private val _balance = MutableStateFlow<Long?>(null)
    val balance: StateFlow<Long?> = _balance.asStateFlow()

    init {
        loadBalance()
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun search() {
        val q = _query.value.trim()
        if (q.isBlank() || _isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _isModerationError.value = false
            try {
                val response = recipeApi.smartSearch(SmartSearchRequestDTO(q))
                _hasSearched.value = true
                if (response.isSuccessful) {
                    _results.value = response.body()?.resources ?: emptyList()
                    loadBalance() // Actualise le solde après débit
                } else {
                    _results.value = emptyList()
                    when (response.code()) {
                        402 -> _error.value = "Solde insuffisant pour effectuer une recherche intelligente."
                        422 -> _isModerationError.value = true
                        else -> _error.value = "Une erreur est survenue (${response.code()}). Veuillez réessayer."
                    }
                }
            } catch (e: Exception) {
                _error.value = "Erreur réseau : ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Sync d'état : appelé depuis MainActivity quand une action
    //    (like, follow, superlike) est déclenchée n'importe où dans l'app.
    //    Permet de refléter les changements dans les résultats de recherche
    //    sans relancer une requête (qui coûterait 0,10 €).

    fun updateLikeState(recipeId: UUID, liked: Boolean) {
        _results.value = _results.value.map { recipe ->
            if (recipe.id == recipeId && recipe.likedByCurrentUser != liked) {
                recipe.copy(
                    likedByCurrentUser = liked,
                    likeCount = if (liked) recipe.likeCount + 1 else recipe.likeCount - 1
                )
            } else recipe
        }
    }

    fun updateFollowState(recipeId: UUID, followed: Boolean) {
        _results.value = _results.value.map { recipe ->
            if (recipe.id == recipeId && recipe.followedByCurrentUser != followed) {
                recipe.copy(
                    followedByCurrentUser = followed,
                    followCount = if (followed) recipe.followCount + 1 else recipe.followCount - 1
                )
            } else recipe
        }
    }

    fun updateSuperLikeState(recipeId: UUID) {
        _results.value = _results.value.map { recipe ->
            if (recipe.id == recipeId && !recipe.superLikedByCurrentUser) {
                recipe.copy(
                    superLikedByCurrentUser = true,
                    superLikeCount = recipe.superLikeCount + 1
                )
            } else recipe
        }
    }

    fun newSearch() {
        _query.value = ""
        _results.value = emptyList()
        _hasSearched.value = false
        _error.value = null
        _isModerationError.value = false
    }

    private fun loadBalance() {
        viewModelScope.launch {
            try {
                val resp = walletApi.getBalance()
                if (resp.isSuccessful) {
                    _balance.value = resp.body()?.balance
                }
            } catch (_: Exception) { /* Silently ignore */ }
        }
    }
}
