package fr.uge.android.forkeat.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.uge.android.forkeat.designsystem.theme.*
import fr.uge.android.forkeat.network.ForkEatApi

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    onNavigateToMyRecipes: () -> Unit = {},
    onNavigateToCreateRecipe: () -> Unit = {},
    onNavigateToWallet: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val isLoggedIn = remember { ForkEatApi.isLoggedIn() }

    if (!isLoggedIn) {
        ProfileGuestScreen(
            onNavigateToLogin = onNavigateToLogin,
            onNavigateToRegister = onNavigateToRegister
        )
        return
    }

    val uiState by profileViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceCream)
            .verticalScroll(rememberScrollState())
    ) {
        ProfileDashboardHeader(uiState = uiState)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileStatsCard(uiState = uiState)
            WalletBalanceCard(uiState = uiState, onNavigateToWallet = onNavigateToWallet)
            ProfileQuickActionsCard(
                onNavigateToMyRecipes = onNavigateToMyRecipes,
                onNavigateToCreateRecipe = onNavigateToCreateRecipe,
                onNavigateToWallet = onNavigateToWallet,
                onNavigateToAccount = onNavigateToAccount
            )
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ProfileDashboardHeader(uiState: ProfileUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Secondary800, Secondary900)
                )
            )
            .padding(bottom = 40.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(48.dp))
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Primary500)
                    .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${uiState.firstName.firstOrNull()?.uppercase() ?: ""}${uiState.lastName.firstOrNull()?.uppercase() ?: ""}",
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
        }
    }
}

@Composable
private fun ProfileStatsCard(uiState: ProfileUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Gray100)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Mes statistiques",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Secondary900,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            StatisticItem(
                icon = Icons.Default.Book,
                iconTint = Secondary700,
                label = "Recettes",
                value = uiState.recipeCount.toString()
            )
            StatisticItem(
                icon = Icons.Default.Favorite,
                iconTint = Primary500,
                label = "Likes reçus",
                value = uiState.totalLikes.toString()
            )
            StatisticItem(
                icon = Icons.Default.Group,
                iconTint = Secondary500,
                label = "Abonnés",
                value = uiState.followers.toString()
            )
            StatisticItem(
                icon = Icons.Default.Star,
                iconTint = Orange500,
                label = "Super Likes",
                value = uiState.superLikes.toString()
            )
        }
    }
}

@Composable
private fun WalletBalanceCard(uiState: ProfileUiState, onNavigateToWallet: () -> Unit) {
    val euros = uiState.walletBalance / 100.0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToWallet),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Gray100)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Primary100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Wallet, contentDescription = null, tint = Primary500)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Mon wallet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Secondary900
                    )
                    Text(
                        text = "Solde disponible",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                }
            }
            Text(
                text = "%.2f €".format(euros),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Primary600
            )
        }
    }
}

@Composable
private fun ProfileQuickActionsCard(
    onNavigateToMyRecipes: () -> Unit,
    onNavigateToCreateRecipe: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToAccount: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Gray100)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Actions rapides",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Secondary900,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            QuickActionItem(
                icon = Icons.Default.Book,
                label = "Mes recettes",
                onClick = onNavigateToMyRecipes
            )
            QuickActionItem(
                icon = Icons.Default.Add,
                label = "Créer une recette",
                onClick = onNavigateToCreateRecipe
            )
            QuickActionItem(
                icon = Icons.Default.Wallet,
                label = "Mon wallet",
                onClick = onNavigateToWallet
            )
            QuickActionItem(
                icon = Icons.Default.ManageAccounts,
                label = "Mon compte",
                onClick = onNavigateToAccount
            )
        }
    }
}

@Composable
private fun StatisticItem(icon: ImageVector, iconTint: Color, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconTint.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Gray500)
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Secondary900)
        }
    }
}

@Composable
private fun QuickActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Secondary50, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Secondary700, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = Secondary900)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Gray500)
    }
}

@Composable
fun ProfileGuestScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceCream)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Secondary50),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = "Icône invité",
                tint = Secondary800,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Bienvenue sur ForkEat",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Secondary900,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Connectez-vous ou créez un compte pour accéder à toutes les fonctionnalités.",
            style = MaterialTheme.typography.bodyLarge,
            color = Secondary600,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Secondary800)
        ) {
            Text("Se connecter", modifier = Modifier.padding(vertical = 8.dp), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, Secondary300)
        ) {
            Text("S'inscrire", modifier = Modifier.padding(vertical = 8.dp), color = Secondary800, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewProfileScreen() {
    ForkEatTheme {
        ProfileScreen()
    }
}
