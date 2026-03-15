package fr.uge.android.forkeat.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.uge.android.forkeat.designsystem.theme.*
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.recipes.EmailNotVerifiedDialog
import fr.uge.android.forkeat.recipes.InsufficientFundsDialog
import fr.uge.android.forkeat.recipes.RecipeCard
import fr.uge.android.forkeat.recipes.data.dto.PersonalizedRecipeSummaryDTO
import kotlin.collections.isNotEmpty
import fr.uge.android.forkeat.recipes.ReportUserDialog

@Composable
fun UserProfileScreen(
    username: String,
    viewModel: UserProfileViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToRecipe: (String) -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToWallet: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val isLoggedIn = remember { ForkEatApi.isLoggedIn() }

    LaunchedEffect(username) {
        viewModel.loadProfile(username)
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            last != null && last.index >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadNextPage()
    }

    // ── Dialogue fonds insuffisants ──────────────────────────────────────────
    if (uiState.insufficientFunds) {
        InsufficientFundsDialog(
            onDismiss = { viewModel.dismissInsufficientFunds() },
            onNavigateToWallet = onNavigateToWallet
        )
    }

    // ── Dialogue email non vérifié ──────────────────────────────────────────
    if (uiState.emailNotVerified) {
        EmailNotVerifiedDialog(
            onDismiss = { viewModel.dismissEmailNotVerified() },
            onNavigateToAccount = onNavigateToAccount,
        )
    }

    // ── Dialogue de signalement ──────────────────────────────────────────────
    if (uiState.showReportDialog) {
        ReportUserDialog(
            onDismiss = { viewModel.dismissReportDialog() },
            onConfirm = { type, justification -> viewModel.reportUser(type, justification) }
        )
    }

    if (uiState.reportSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissReportSuccess() },
            title = { Text("Signalement envoyé", fontWeight = FontWeight.Bold, color = Secondary900) },
            text = { Text("Votre signalement a bien été enregistré. Notre équipe va l'examiner.", color = Gray500) },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissReportSuccess() },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary500),
                    shape = RoundedCornerShape(50)
                ) { Text("OK", fontWeight = FontWeight.Bold) }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    if (uiState.reportAlreadyDone) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissReportAlreadyDone() },
            title = { Text("Déjà signalé", fontWeight = FontWeight.Bold, color = Secondary900) },
            text = { Text("Vous avez déjà signalé cet utilisateur.", color = Gray500) },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissReportAlreadyDone() },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary500),
                    shape = RoundedCornerShape(50)
                ) { Text("OK", fontWeight = FontWeight.Bold) }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceCream)
    ) {
        item {
            UserProfileHeader(
                uiState = uiState,
                onNavigateBack = onNavigateBack,
                onFollow = { viewModel.follow() },
                onUnfollow = { viewModel.unfollow() },
                onReport = { viewModel.showReportDialog() }
            )
        }

        if (uiState.isLoading && uiState.recipes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary500)
                }
            }
        }

        uiState.error?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (uiState.recipes.isNotEmpty()) {
            item {
                Text(
                    text = "${uiState.totalRecipes} recettes",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Secondary900,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                )
            }
            items(uiState.recipes) { recipe ->
                RecipeCard(
                    recipe = recipe,
                    onRecipeClick = { onNavigateToRecipe(recipe.id.toString()) },
                    onUsernameClick = { /* Déjà sur le profil */ },
                    isLoggedIn = isLoggedIn,
                    onNavigateToLogin = onNavigateToLogin,
                    onLike = { viewModel.likeRecipe(it) },
                    onUnlike = { viewModel.unlikeRecipe(it) },
                    onSuperLike = { viewModel.superLikeRecipe(it) },
                    onFollow = { viewModel.followRecipe(it) },
                    onUnfollow = { viewModel.unfollowRecipe(it) }
                )
            }
            if (uiState.isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Primary500, modifier = Modifier.size(24.dp))
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun UserProfileHeader(
    uiState: UserProfileUiState,
    onNavigateBack: () -> Unit,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
    onReport: () -> Unit
) {
    val currentUsername = remember { ForkEatApi.getCurrentUsername() }
    val isOwnProfile = uiState.username == currentUsername
    val isLoggedIn = ForkEatApi.isLoggedIn()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Secondary800, Secondary900)
                )
            )
            .padding(bottom = 32.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.weight(1f))
                if (isLoggedIn && !isOwnProfile) {
                    IconButton(onClick = onReport) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Signaler cet utilisateur",
                            tint = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Primary500)
                        .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = if (uiState.firstName.isNotEmpty() && uiState.lastName.isNotEmpty()) {
                        "${uiState.firstName.first()}${uiState.lastName.first()}".uppercase()
                    } else if (uiState.username.isNotEmpty()) {
                        uiState.username.take(2).uppercase()
                    } else ""

                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "${uiState.firstName} ${uiState.lastName}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "@${uiState.username}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Secondary100
                )
                
                if (isLoggedIn && !isOwnProfile) {
                    Spacer(Modifier.height(20.dp))
                    if (uiState.followedByCurrentUser) {
                        OutlinedButton(
                            onClick = onUnfollow,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("Suivi", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onFollow,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary500, contentColor = Color.White),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("Suivre", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserProfileStatItem(label = "Recettes", value = uiState.totalRecipes.toString())
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Secondary700))
                    UserProfileStatItem(label = "Abonnés", value = uiState.followerCount.toString())
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Secondary700))
                    UserProfileStatItem(label = "Abonnements", value = uiState.followingCount.toString())
                }
            }
        }
    }
}

@Composable
private fun UserProfileStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Secondary300
        )
    }
}
