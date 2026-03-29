package fr.uge.android.forkeat.recipes

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.uge.android.forkeat.designsystem.theme.Gray500
import fr.uge.android.forkeat.designsystem.theme.Primary500
import fr.uge.android.forkeat.designsystem.theme.Secondary900
import fr.uge.android.forkeat.designsystem.theme.SurfaceCream
import fr.uge.android.forkeat.designsystem.theme.Typography
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.promotions.data.dto.PromotionDTO
import fr.uge.android.forkeat.recipes.data.dto.PersonalizedRecipeSummaryDTO
import java.util.UUID

// ── Enum ─────────────────────────────────────────────────────────────────────

enum class FeedMode { GENERAL, PERSONAL }

// ── Écran principal ───────────────────────────────────────────────────────────

@Composable
fun RecipesListScreen(
    recipes: List<PersonalizedRecipeSummaryDTO>,
    recipesFeed: List<PersonalizedRecipeSummaryDTO>,
    totalCount: Int,
    onLoadMore: () -> Unit,
    onLoadMoreFeed: () -> Unit,
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
    superLikeBasePriceCents: Long? = null,
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
    activePromotion: PromotionDTO? = null,
    initialFeedMode: FeedMode = FeedMode.GENERAL,
    onFeedSelected: ()-> Unit = {}
) {

    var feedMode by remember { mutableStateOf(initialFeedMode) }

    val onFeedModeChange = { mode: FeedMode ->
        feedMode = mode
        if(mode.equals(FeedMode.PERSONAL)){
            onFeedSelected.invoke();
        }
    }
    val listState = rememberLazyListState()
    val currentUsername = remember(isLoggedIn) { if (isLoggedIn) ForkEatApi.getCurrentUsername() else null }

    // ── Dialogue fonds insuffisants ──────────────────────────────────────────
    if (insufficientFunds) {
        InsufficientFundsDialog(
            onDismiss = onDismissInsufficientFunds,
            onNavigateToWallet = onNavigateToWallet
        )
    }

    // ── Dialogue email non vérifié ───────────────────────────────────────────
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
            if(feedMode.equals(FeedMode.GENERAL)){
                RecipeSearchFilterBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearchSubmit = onSearchSubmit,
                    availableAllergens = availableAllergens,
                    selectedAllergens = selectedAllergens,
                    onAllergenToggle = onAllergenToggle,
                    onClearFilters = onClearFilters
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Toggle Général / Pour moi ────────────────────────────────────
            FeedToggle(
                feedMode = feedMode,
                isLoggedIn = isLoggedIn,
                onFeedModeChange = onFeedModeChange,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (activePromotion != null) {
                Spacer(modifier = Modifier.height(8.dp))
                PromotionBanner(promotion = activePromotion)
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

            if(feedMode.equals(FeedMode.PERSONAL) && recipesFeed.isEmpty()){
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DynamicFeed,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aucune recette disponible dans votre feed personnalisé",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Suivez des personnes pour le remplir",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            }else{
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState
                ) {
                    val recipesToIndex:  List<PersonalizedRecipeSummaryDTO> = if(feedMode.equals(FeedMode.GENERAL))  recipes else recipesFeed;
                    itemsIndexed(recipesToIndex) { index, recipe ->
                        RecipeCard(
                            recipe = if (!isLoggedIn) recipe.copy(
                                likedByCurrentUser = false,
                                superLikedByCurrentUser = false,
                                followedByCurrentUser = false
                            ) else recipe,
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
                            superLikeBasePriceCents = superLikeBasePriceCents,
                            superLikePromoPriceCents = activePromotion?.priceCents,
                            onFollow = onFollowRecipe,
                            onUnfollow = onUnfollowRecipe,
                        )
                        if (index == recipesToIndex.lastIndex) {
                            if(feedMode.equals(FeedMode.GENERAL)){
                                LaunchedEffect(recipes.size) {
                                    onLoadMore()
                                }
                            }else{
                                LaunchedEffect(recipes.size) {
                                    onLoadMoreFeed()
                                }
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = Primary500)
                            }
                        }
                    }

                    if (recipes.size >= totalCount && totalCount > 0) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
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

// ── Toggle pill ───────────────────────────────────────────────────────────────

@Composable
fun FeedToggle(
    feedMode: FeedMode,
    isLoggedIn: Boolean,
    onFeedModeChange: (FeedMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Gray500.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50)
            )
            .padding(4.dp)
    ) {
        Row {
            FeedToggleOption(
                label = "Général",
                selected = feedMode == FeedMode.GENERAL,
                enabled = true,
                onClick = { onFeedModeChange(FeedMode.GENERAL) }
            )
            FeedToggleOption(
                label = "Pour moi",
                selected = feedMode == FeedMode.PERSONAL,
                enabled = isLoggedIn,
                onClick = { if (isLoggedIn) onFeedModeChange(FeedMode.PERSONAL) }
            )
        }
    }
}

@Composable
private fun FeedToggleOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) Primary500 else Color.Transparent,
        label = "toggle_bg"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            selected -> Color.White
            !enabled -> Gray500.copy(alpha = 0.4f)
            else -> Secondary900
        },
        label = "toggle_text"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = Typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

// ── Bannière promotion ────────────────────────────────────────────────────────

@Composable
fun PromotionBanner(promotion: PromotionDTO) {
    val price = String.format("%.2f€", promotion.priceCents / 100.0)
    val bonusText = promotion.bonusEveryN?.let { " · 1 gratuit tous les $it" } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFFF6B35),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Promotion en cours !",
                style = Typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "${promotion.name} · Super-like à $price$bonusText",
                style = Typography.bodySmall,
                color = Color.White
            )
        }
    }
}