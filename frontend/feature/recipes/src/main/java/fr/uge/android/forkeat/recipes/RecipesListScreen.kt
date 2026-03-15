package fr.uge.android.forkeat.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import fr.uge.android.forkeat.recipes.data.dto.PersonalizedRecipeSummaryDTO
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.uge.android.forkeat.designsystem.InfiniteListHandler
import fr.uge.android.forkeat.designsystem.theme.Gray500
import fr.uge.android.forkeat.designsystem.theme.Primary500
import fr.uge.android.forkeat.designsystem.theme.Secondary900
import fr.uge.android.forkeat.designsystem.theme.SurfaceCream
import fr.uge.android.forkeat.designsystem.theme.Typography
import fr.uge.android.forkeat.network.ForkEatApi
import java.util.UUID

@Composable
fun RecipesListScreen(
    recipes: List<PersonalizedRecipeSummaryDTO>,
    totalCount: Int,
    onLoadMore: () -> Unit,
    errorMessage: String? = null,
    isLoading: Boolean = false,
    isLoggedIn: Boolean = false,
    onNavigateToCreateRecipe: () -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchSubmit: () -> Unit = {},
    availableAllergens: List<String> = emptyList(),
    selectedAllergens: Set<String> = emptySet(),
    onAllergenToggle: (String) -> Unit = {},
    onClearFilters: () -> Unit = {},
    onRecipeClick: (String) -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToMyProfile: () -> Unit = {},
    onLikeRecipe: (UUID) -> Unit = {},
    onUnlikeRecipe: (UUID) -> Unit = {},
    onSuperLikeRecipe: (UUID) -> Unit = {},
    onFollowRecipe: (UUID) -> Unit = {},
    onUnfollowRecipe: (UUID) -> Unit = {},
    insufficientFunds: Boolean = false,
    onDismissInsufficientFunds: () -> Unit = {},
    emailNotVerified: Boolean = false,
    onDismissEmailNotVerified: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToWallet: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToSmartSearch: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val currentUsername = remember(isLoggedIn) { if (isLoggedIn) ForkEatApi.getCurrentUsername() else null }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 1
        }
    }
    LaunchedEffect(shouldLoadMore, recipes.size, totalCount) {
        if (shouldLoadMore && recipes.size < totalCount) {
            onLoadMore()
        }
    }

    // ── Dialogue fonds insuffisants ──────────────────────────────────────────
    if (insufficientFunds) {
        InsufficientFundsDialog(
            onDismiss = onDismissInsufficientFunds,
            onNavigateToWallet = onNavigateToWallet
        )
    }

    // ── Dialogue email non vérifié ──────────────────────────────────────────
    if (emailNotVerified) {
        EmailNotVerifiedDialog(
            onDismiss = onDismissEmailNotVerified,
            onNavigateToAccount = onNavigateToAccount,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceCream)
                .padding(16.dp)
        ) {
            RecipeSearchFilterBar(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSearchSubmit = onSearchSubmit,
                availableAllergens = availableAllergens,
                selectedAllergens = selectedAllergens,
                onAllergenToggle = onAllergenToggle,
                onClearFilters = onClearFilters
            )

            if (isLoggedIn) {
                OutlinedButton(
                    onClick = onNavigateToSmartSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary500),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary500.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Recherche Intelligente par IA",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Décrivez votre envie en langage naturel — 0,10 €",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gray500
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState
            ) {
                items(recipes) { recipe ->
                    RecipeCard(
                        recipe = if (!isLoggedIn) recipe.copy(likedByCurrentUser = false, superLikedByCurrentUser = false, followedByCurrentUser = false) else recipe,
                        onRecipeClick = { onRecipeClick(recipe.id.toString()) },
                        onUsernameClick = { username ->
                            if (!isLoggedIn) {
                                onNavigateToLogin()
                            } else if (currentUsername != null && username == currentUsername) {
                                onNavigateToMyProfile()
                            } else {
                                onNavigateToUserProfile(username)
                            }
                        },
                        isLoggedIn = isLoggedIn,
                        onNavigateToLogin = onNavigateToLogin,
                        onLike = onLikeRecipe,
                        onUnlike = onUnlikeRecipe,
                        onSuperLike = onSuperLikeRecipe,
                        onFollow = onFollowRecipe,
                        onUnfollow = onUnfollowRecipe,
                    )
                }
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = Primary500)
                        }
                    }
                }
                if (recipes.size >= totalCount && totalCount > 0) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Toutes les recettes ont été chargées",
                                style = Typography.labelLarge,
                                color = Secondary900
                            )
                        }
                    }
                }
            }
        }

        if (isLoggedIn) {
            FloatingActionButton(
                onClick = onNavigateToCreateRecipe,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = Primary500,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Créer une recette")
            }
        }
    }
}