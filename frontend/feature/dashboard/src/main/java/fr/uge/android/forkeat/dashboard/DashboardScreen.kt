package fr.uge.android.forkeat.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.uge.android.forkeat.designsystem.theme.ForkEatTheme
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class) // Added OptIn annotation
@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel = viewModel(),
    onNavigateToProfile: () -> Unit,
    onNavigateToWallet: () -> Unit = {}
) {
    val uiState by dashboardViewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                dashboardViewModel.onRefreshDashboard()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(key1 = true) {
        dashboardViewModel.navigationEvent.collect { event ->
            when (event) {
                is DashboardNavigationEvent.NavigateToProfile -> onNavigateToProfile()
                DashboardNavigationEvent.NavigateToCreateRecipe -> TODO()
                DashboardNavigationEvent.NavigateToWallet -> onNavigateToWallet()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
    ) {
        // Welcome Section
        DashboardWelcomeSection(uiState.firstName)

            // Statistics Section
            DashboardStatisticsSection(
                balance = uiState.balance,
                totalRecipes = uiState.totalRecipes,
                totalLikes = uiState.totalLikes,
                followers = uiState.followers,
                onWalletClick = { dashboardViewModel.navigateToWallet() }
            )

            // Quick Actions Section
            DashboardQuickActionsSection(
                onCreateRecipeClick = { dashboardViewModel.navigateToCreateRecipe() },
                onManageWalletClick = { dashboardViewModel.navigateToWallet() },
                onProfileClick = { dashboardViewModel.navigateToProfile() }
            )

            // Recent Activity Placeholder
            DashboardRecentActivityPlaceholder()
        }
}

@Composable
fun DashboardWelcomeSection(firstName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)) // Mimic from-secondary-800 to-secondary-900
                )
            )
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        Column {
            Text(
                text = "Bienvenue, $firstName ! 👋",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Ravi de vous revoir. Voici un aperçu de votre activité.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5E1) // Mimic text-secondary-200
            )
        }
    }
}

@Composable
fun DashboardStatisticsSection(
    balance: Double,
    totalRecipes: Int,
    totalLikes: Int,
    followers: Int,
    onWalletClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Wallet Balance
            StatisticCard(
                modifier = Modifier.weight(1f),
                title = "Solde disponible",
                value = "${DecimalFormat("0.00").format(balance)} €",
                icon = Icons.Default.Wallet,
                iconTint = Color.White,
                backgroundColor = Brush.linearGradient(
                    colors = listOf(Color(0xFFF97316), Color(0xFFEA580C)) // Mimic from-primary-500 to-primary-600
                ),
                onClick = onWalletClick,
                valueColor = Color.White,
                titleColor = Color.White.copy(alpha = 0.8f)
            )
            // My Recipes
            StatisticCard(
                modifier = Modifier.weight(1f),
                title = "Mes recettes",
                value = totalRecipes.toString(),
                icon = Icons.Default.Book,
                iconTint = Color(0xFF1E293B), // Mimic text-secondary-800
                backgroundColor = Brush.linearGradient(colors = listOf(Color.White, Color.White)), // Changed to Brush
                borderColor = Color(0xFFE5E7EB) // Mimic border-gray-100
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Total Likes
            StatisticCard(
                modifier = Modifier.weight(1f),
                title = "Total likes",
                value = totalLikes.toString(),
                icon = Icons.Default.Favorite,
                iconTint = Color(0xFFEF4444), // Mimic text-red-500
                backgroundColor = Brush.linearGradient(colors = listOf(Color.White, Color.White)), // Changed to Brush
                borderColor = Color(0xFFE5E7EB)
            )
            // Followers
            StatisticCard(
                modifier = Modifier.weight(1f),
                title = "Abonnés",
                value = followers.toString(),
                icon = Icons.Default.Group,
                iconTint = Color(0xFF3B82F6), // Mimic text-blue-500
                backgroundColor = Brush.linearGradient(colors = listOf(Color.White, Color.White)), // Changed to Brush
                borderColor = Color(0xFFE5E7EB)
            )
        }
    }
}

@Composable
fun StatisticCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    backgroundColor: Brush, // Changed from Color to Brush
    borderColor: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    valueColor: Color = Color(0xFF1E293B), // text-secondary-900
    titleColor: Color = Color(0xFF6B7280) // text-gray-500
) {
    Card(
        modifier = modifier
            .height(150.dp) // Fixed height for consistency
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = if (borderColor != Color.Transparent) BorderStroke(1.dp, borderColor) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(iconTint.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    if (onClick != null) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward, // Represents the arrow icon
                            contentDescription = "Go to ${title}",
                            tint = titleColor.copy(alpha = 0.8f) // text-white/80 or similar
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = valueColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = titleColor
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)) // Mimic border-gray-200
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(iconBackground, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1E293B) // Mimic text-secondary-900
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280) // Mimic text-gray-600
                )
            }
        }
    }
}

@Composable
fun DashboardQuickActionsSection(
    onCreateRecipeClick: () -> Unit,
    onManageWalletClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Actions rapides",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1E293B), // Mimic text-secondary-900
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = "Créer une recette",
                description = "Partagez une nouvelle création culinaire",
                icon = Icons.Default.Add,
                iconBackground = Color(0xFFFCE7F3), // Mimic bg-primary-50
                iconTint = Color(0xFFF97316), // Mimic text-primary-500
                onClick = onCreateRecipeClick
            )
            QuickActionCard(
                title = "Gérer mon wallet",
                description = "Rechargez ou consultez vos transactions",
                icon = Icons.Default.Wallet,
                iconBackground = Color(0xFFEEF2FF), // Mimic bg-secondary-50
                iconTint = Color(0xFF1E293B), // Mimic text-secondary-800
                onClick = onManageWalletClick
            )
            QuickActionCard(
                title = "Paramètres",
                description = "Modifiez votre profil et vos préférences",
                icon = Icons.Default.Settings,
                iconBackground = Color(0xFFFFF7ED), // Mimic bg-orange-50
                iconTint = Color(0xFFF97316), // Mimic text-orange-500
                onClick = onProfileClick
            )
        }
    }
}

@Composable
fun DashboardRecentActivityPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Activité récente",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1E293B), // Mimic text-secondary-900
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home, // Placeholder for inbox icon
                    contentDescription = null,
                    tint = Color(0xFFD1D5DB), // Mimic text-gray-300
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Aucune activité récente pour le moment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280) // Mimic text-gray-500
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Créez votre première recette pour commencer !",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF) // Mimic text-gray-400
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDashboardScreen() {
    ForkEatTheme {
        DashboardScreen(
            dashboardViewModel = TODO(),
            onNavigateToProfile = TODO()
        )
    }
}
