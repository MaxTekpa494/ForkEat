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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.uge.android.forkeat.promotions.data.dto.PromotionDTO

@Composable
fun AdminPromotionsScreen(
    currentRoute: String,
    onNavigateToDashboard: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onNavigateToWallets: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToPromotions: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToCreatePromotion: () -> Unit,
    onNavigateToEditPromotion: (String) -> Unit,
    onNavigateToSuperLikeConfig: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminPromotionsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var confirmCancelId by remember { mutableStateOf<String?>(null) }

    if (confirmCancelId != null) {
        AlertDialog(
            onDismissRequest = { confirmCancelId = null },
            title = { Text("Annuler la promotion ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancel(confirmCancelId!!)
                    confirmCancelId = null
                }) { Text("Confirmer", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancelId = null }) { Text("Retour") }
            }
        )
    }

    uiState.error?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Erreur") },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("OK") } }
        )
    }

    AdminScaffold(
        currentRoute = currentRoute,
        onNavigateToDashboard = onNavigateToDashboard,
        onNavigateToUsers = onNavigateToUsers,
        onNavigateToRecipes = onNavigateToRecipes,
        onNavigateToWallets = onNavigateToWallets,
        onNavigateToCreate = onNavigateToCreate,
        onNavigateToPromotions = onNavigateToPromotions,
        onNavigateToReports = onNavigateToReports,
        onLogout = onLogout
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AdminPurple100)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Promotions", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AdminPurple900)
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onNavigateToSuperLikeConfig)
                            .background(AdminPurple200, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = AdminPurple700, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Prix & ratio super-like", fontSize = 11.sp, color = AdminPurple700, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (uiState.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AdminPurple500)
                    }
                } else if (uiState.promotions.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucune promotion", color = AdminPurple700)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(uiState.promotions) { promo ->
                            PromotionCard(
                                promotion = promo,
                                onEdit = { onNavigateToEditPromotion(promo.id) },
                                onCancel = { confirmCancelId = promo.id }
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onNavigateToCreatePromotion,
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                containerColor = AdminPurple600,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouvelle promotion")
            }
        }
    }
}

@Composable
private fun PromotionCard(
    promotion: PromotionDTO,
    onEdit: () -> Unit,
    onCancel: () -> Unit
) {
    val (statusColor, statusLabel) = when (promotion.status) {
        "ACTIVE"    -> Color(0xFF16A34A) to "ACTIVE"
        "SCHEDULED" -> Color(0xFF2563EB) to "PLANIFIÉE"
        "EXPIRED"   -> Color(0xFF6B7280) to "EXPIRÉE"
        else        -> Color(0xFFDC2626) to "ANNULÉE"
    }
    val price = String.format("%.2f€", promotion.priceCents / 100.0)
    val isScheduled = promotion.status == "SCHEDULED"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(promotion.name, fontWeight = FontWeight.Bold, color = AdminPurple900, fontSize = 16.sp,
                modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Prix : $price", color = AdminPurple700, fontSize = 13.sp)
        promotion.bonusEveryN?.let {
            Text("Bonus : 1 gratuit tous les $it", color = AdminPurple700, fontSize = 13.sp)
        }
        Text("Début : ${promotion.startsAt?.take(16)?.replace("T", " ") ?: "-"}", color = Color.Gray, fontSize = 12.sp)
        promotion.endsAt?.let {
            Text("Fin : ${it.take(16).replace("T", " ")}", color = Color.Gray, fontSize = 12.sp)
        }

        if (isScheduled) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = AdminPurple600, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Modifier", color = AdminPurple600)
                }
                TextButton(onClick = onCancel) {
                    Icon(Icons.Default.Cancel, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Annuler", color = Color.Red)
                }
            }
        }
    }
}
