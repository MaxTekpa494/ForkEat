package fr.uge.android.forkeat.admin

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    currentRoute: String,
    onNavigateToMembers: () -> Unit,
    onNavigateToModerators: () -> Unit,
    onNavigateToAdmins: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onNavigateToReports : () -> Unit,
    onNavigateToWallets: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminDashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    AdminScaffold(
        currentRoute = currentRoute,
        onNavigateToDashboard = {},
        onNavigateToUsers = onNavigateToMembers,
        onNavigateToRecipes = onNavigateToRecipes,
        onNavigateToReports = onNavigateToReports,
        onNavigateToWallets = onNavigateToWallets,
        onNavigateToCreate = onNavigateToCreate,
        onLogout = onLogout
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadDashboard() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F3FF))
                    .verticalScroll(rememberScrollState())
            ) {
                // En-tête violet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AdminPurple900, AdminPurple800)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Column {
                        Text(
                            text = "Tableau de bord",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Vue d'ensemble de la plateforme",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AdminPurple300
                        )
                    }
                }

                if (uiState.error != null) {
                    ErrorBanner(message = uiState.error!!, onRetry = { viewModel.loadDashboard() })
                }

                // Section utilisateurs
                SectionTitle(title = "Utilisateurs", modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Membres",
                        value = uiState.userStats?.memberCount?.toString() ?: "—",
                        icon = Icons.Default.Person,
                        iconTint = AdminPurple500,
                        iconBg = AdminPurple100,
                        onClick = onNavigateToMembers
                    )
                    AdminStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Modérateurs",
                        value = uiState.userStats?.moderatorCount?.toString() ?: "—",
                        icon = Icons.Default.Shield,
                        iconTint = Color(0xFF0891B2),
                        iconBg = Color(0xFFE0F2FE),
                        onClick = onNavigateToModerators
                    )
                    AdminStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Admins",
                        value = uiState.userStats?.adminCount?.toString() ?: "—",
                        icon = Icons.Default.Group,
                        iconTint = AdminPurple700,
                        iconBg = AdminPurple200,
                        onClick = onNavigateToAdmins
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Section recettes
                SectionTitle(title = "Recettes", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Publiées",
                        value = uiState.recipeStats?.published?.toString() ?: "—",
                        icon = Icons.Default.Check,
                        iconTint = Color(0xFF16A34A),
                        iconBg = Color(0xFFDCFCE7),
                        onClick = onNavigateToRecipes
                    )
                    AdminStatCard(
                        modifier = Modifier.weight(1f),
                        label = "En attente",
                        value = uiState.recipeStats?.pending?.toString() ?: "—",
                        icon = Icons.Default.HourglassEmpty,
                        iconTint = Color(0xFFD97706),
                        iconBg = Color(0xFFFEF3C7),
                        onClick = onNavigateToRecipes
                    )
                    AdminStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Brouillons",
                        value = uiState.recipeStats?.draft?.toString() ?: "—",
                        icon = Icons.Default.Book,
                        iconTint = Color(0xFF6B7280),
                        iconBg = Color(0xFFF3F4F6)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Section wallets plateforme
                SectionTitle(title = "Portefeuilles plateforme", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WalletSummaryCard(
                        label = "Gains (EARNINGS)",
                        balanceCents = uiState.benefitsWallet?.balance,
                        gradient = Brush.linearGradient(
                            colors = listOf(AdminPurple800, AdminPurple600)
                        ),
                        onClick = onNavigateToWallets
                    )
                    WalletSummaryCard(
                        label = "Redistribution",
                        balanceCents = uiState.redistributionWallet?.balance,
                        gradient = Brush.linearGradient(
                            colors = listOf(Color(0xFF0369A1), Color(0xFF0284C7))
                        ),
                        onClick = onNavigateToWallets
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Composables utilitaires ──────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = AdminPurple900,
        modifier = modifier
    )
}

@Composable
fun AdminStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1E293B))
            Text(text = label, fontSize = 11.sp, color = Color(0xFF6B7280))
        }
    }
}

@Composable
private fun WalletSummaryCard(
    label: String,
    balanceCents: Long?,
    gradient: Brush,
    onClick: (() -> Unit)? = null
) {
    val fmt = DecimalFormat("0.00")
    Card(
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (balanceCents != null) "${fmt.format(balanceCents / 100.0)} €" else "—",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Wallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = message, color = Color(0xFFDC2626), fontSize = 13.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onRetry) {
            Text("Réessayer", color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
        }
    }
}
