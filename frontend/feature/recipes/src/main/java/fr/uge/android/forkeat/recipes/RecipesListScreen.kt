package fr.uge.android.forkeat.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.uge.android.forkeat.designsystem.theme.*
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.recipes.data.dto.PersonalizedRecipeSummaryDTO
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
    insufficientFunds: Boolean = false,
    onDismissInsufficientFunds: () -> Unit = {},
    emailNotVerified: Boolean = false,
    onDismissEmailNotVerified: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToWallet: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
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
                availableAllergens = emptyList(),
                selectedAllergens = selectedAllergens,
                onAllergenToggle = onAllergenToggle,
                onClearFilters = onClearFilters
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (availableAllergens.isNotEmpty()) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "Exclure les allergènes :",
                        style = MaterialTheme.typography.labelMedium,
                        color = Secondary700,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(availableAllergens) { allergen ->
                            val isSelected = selectedAllergens.contains(allergen)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onAllergenToggle(allergen) },
                                label = { Text(allergen, fontSize = 12.sp) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary500,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White,
                                    labelColor = Gray500
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState
            ) {
                items(recipes) { recipe ->
                    RecipeCard(
                        recipe = if (!isLoggedIn) recipe.copy(likedByCurrentUser = false, superLikedByCurrentUser = false) else recipe,
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
