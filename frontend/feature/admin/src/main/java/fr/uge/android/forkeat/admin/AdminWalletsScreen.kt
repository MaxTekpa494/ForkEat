package fr.uge.android.forkeat.admin

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import fr.uge.android.forkeat.admin.data.dto.PlatformWalletDTO
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWalletsScreen(
    currentRoute: String,
    onNavigateToDashboard: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onNavigateToReports : () -> Unit,
    onNavigateToCreate: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminWalletsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    AdminScaffold(
        currentRoute = currentRoute,
        onNavigateToDashboard = onNavigateToDashboard,
        onNavigateToUsers = onNavigateToUsers,
        onNavigateToRecipes = onNavigateToRecipes,
        onNavigateToReports = onNavigateToReports,
        onNavigateToWallets = {},
        onNavigateToCreate = onNavigateToCreate,
        onLogout = onLogout
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadWallets() },
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
                // En-tête
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AdminPurple900)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Column {
                        Text("Portefeuilles plateforme", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        Spacer(Modifier.height(2.dp))
                        Text("Suivi des fonds de la plateforme", fontSize = 12.sp, color = AdminPurple300)
                    }
                }

                if (uiState.error != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = uiState.error!!, color = Color(0xFFDC2626), fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Wallet EARNINGS
                PlatformWalletDetailCard(
                    wallet = uiState.benefitsWallet,
                    title = "Gains (EARNINGS)",
                    subtitle = "Commissions et frais de service collectés",
                    icon = Icons.Default.TrendingUp,
                    gradient = Brush.linearGradient(
                        colors = listOf(AdminPurple800, AdminPurple600)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(16.dp))

                // Wallet REDISTRIBUTION
                PlatformWalletDetailCard(
                    wallet = uiState.redistributionWallet,
                    title = "Redistribution",
                    subtitle = "Fonds à redistribuer aux créateurs",
                    icon = Icons.Default.Wallet,
                    gradient = Brush.linearGradient(
                        colors = listOf(Color(0xFF0369A1), Color(0xFF0284C7))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PlatformWalletDetailCard(
    wallet: PlatformWalletDTO?,
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    val fmt = DecimalFormat("0.00")
    val balanceEuros = wallet?.balance?.let { it / 100.0 }
    val formattedBalance = if (balanceEuros != null) "${fmt.format(balanceEuros)} €" else "—"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(text = subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = Color.White.copy(alpha = 0.2f)
                )

                // Solde
                Text(text = "Solde total", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formattedBalance,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )

                if (wallet?.updatedAt != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(50))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Mis à jour : ${wallet.updatedAt.take(10)}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
