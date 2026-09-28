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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import fr.uge.android.forkeat.admin.data.dto.PlatformWalletTransactionDTO
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
    onNavigateToPromotions: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminWalletsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Lancer la redistribution") },
            text = { Text("Déclencher manuellement la redistribution ?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.triggerRedistribution()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminPurple800)
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    AdminScaffold(
        currentRoute = currentRoute,
        onNavigateToDashboard = onNavigateToDashboard,
        onNavigateToUsers = onNavigateToUsers,
        onNavigateToRecipes = onNavigateToRecipes,
        onNavigateToReports = onNavigateToReports,
        onNavigateToWallets = {},
        onNavigateToCreate = onNavigateToCreate,
        onNavigateToPromotions = onNavigateToPromotions,
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

                if (uiState.redistributionSuccess) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Redistribution lancée avec succès.", color = Color(0xFF16A34A), fontSize = 13.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.dismissRedistributionFeedback() }) {
                            Text("OK", color = Color(0xFF16A34A), fontSize = 12.sp)
                        }
                    }
                }

                if (uiState.redistributionError != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(uiState.redistributionError!!, color = Color(0xFFDC2626), fontSize = 13.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.dismissRedistributionFeedback() }) {
                            Text("OK", color = Color(0xFFDC2626), fontSize = 12.sp)
                        }
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

                Spacer(Modifier.height(16.dp))

                // Bouton redistribution
                OutlinedButton(
                    onClick = { showConfirmDialog = true },
                    enabled = !uiState.redistributionTriggering,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminPurple800)
                ) {
                    if (uiState.redistributionTriggering) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AdminPurple800)
                        Spacer(Modifier.width(8.dp))
                        Text("Redistribution en cours…", fontSize = 14.sp)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Lancer la redistribution", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Transaction history
                TransactionHistorySection(
                    transactions = uiState.transactions,
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
private fun TransactionHistorySection(
    transactions: List<PlatformWalletTransactionDTO>,
    modifier: Modifier = Modifier
) {
    val fmt = DecimalFormat("0.00")
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = AdminPurple800, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Historique des transactions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AdminPurple900)
            }
            Spacer(Modifier.height(12.dp))
            if (transactions.isEmpty()) {
                Text("Aucune transaction", color = Color(0xFF9CA3AF), fontSize = 13.sp)
            } else {
                transactions.forEach { tx ->
                    TransactionRow(tx, fmt)
                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: PlatformWalletTransactionDTO, fmt: DecimalFormat) {
    val (badgeBackground, badgeText, icon) = when (tx.reason) {
        "ACCOUNT_DELETION" -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), Icons.Default.PersonOff)
        "SUPER_LIKE_EARNED" -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), Icons.Default.Star)
        else -> Triple(Color(0xFFF3F4F6), Color(0xFF6B7280), Icons.Default.History)
    }
    val label = when (tx.reason) {
        "ACCOUNT_DELETION" -> "Suppression compte"
        "SUPER_LIKE_EARNED" -> "SuperLike gagné"
        "SUPER_LIKE_REDISTRIBUTION" -> "Redistribution"
        "BONUS_FINANCED" -> "Bonus financé"
        else -> tx.reason
    }
    val amountStr = if (tx.amountCents >= 0) "+${fmt.format(tx.amountCents / 100.0)} €"
    else "${fmt.format(tx.amountCents / 100.0)} €"
    val amountColor = if (tx.amountCents >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(badgeBackground, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = badgeText, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111827)
            )
            Text(
                text = tx.createdAt.take(10),
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF)
            )
        }
        Text(
            text = amountStr,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
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
