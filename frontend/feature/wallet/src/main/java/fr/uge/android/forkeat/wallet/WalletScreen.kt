package fr.uge.android.forkeat.wallet

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.uge.android.forkeat.network.dto.wallet.TransactionDTO
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    walletViewModel: WalletViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by walletViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh wallet data when returning from Stripe Custom Tab
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                walletViewModel.loadWallet()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Open Stripe Checkout in Custom Tab
    LaunchedEffect(uiState.rechargeUrl) {
        uiState.rechargeUrl?.let { url ->
            openCustomTab(context, url)
            walletViewModel.onRechargeUrlConsumed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFBF9F4))
    ) {
            if (uiState.isLoading && uiState.transactions.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFFFF5745)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Wallet Card
                    WalletCard(balance = uiState.balance)

                    // Quick Actions
                    QuickActions(
                        onRechargeClick = { walletViewModel.showRechargeDialog() },
                        onWithdrawClick = { walletViewModel.showWithdrawDialog() }
                    )

                    // Bank Info Section
                    BankInfoSection(
                        bankInfo = uiState.bankInfo,
                        onAddClick = { walletViewModel.showBankInfoForm() }
                    )

                    // Error message
                    uiState.error?.let { error ->
                        ErrorBanner(error, onDismiss = { walletViewModel.dismissError() })
                    }

                    // Success message
                    uiState.successMessage?.let { msg ->
                        SuccessBanner(msg, onDismiss = { walletViewModel.onSuccessMessageConsumed() })
                    }

                    // Transactions
                    TransactionsSection(transactions = uiState.transactions)

                    Spacer(Modifier.height(24.dp))
                }
            }

            // Recharge Dialog
            if (uiState.showRechargeDialog) {
                RechargeDialog(
                    isLoading = uiState.isLoading,
                    onDismiss = { walletViewModel.dismissRechargeDialog() },
                    onConfirm = { amount -> walletViewModel.recharge(amount) }
                )
            }

            // Withdraw Dialog
            if (uiState.showWithdrawDialog) {
                WithdrawDialog(
                    balanceCents = uiState.balance,
                    hasBankInfo = uiState.bankInfo != null,
                    bankName = uiState.bankInfo?.bankName,
                    isLoading = uiState.isLoading,
                    onDismiss = { walletViewModel.dismissWithdrawDialog() },
                    onConfirm = { amount -> walletViewModel.withdraw(amount) },
                    onAddBankInfo = {
                        walletViewModel.dismissWithdrawDialog()
                        walletViewModel.showBankInfoForm()
                    }
                )
            }

            // Bank Info Form Dialog
            if (uiState.showBankInfoForm) {
                BankInfoFormDialog(
                    isLoading = uiState.isLoading,
                    onDismiss = { walletViewModel.dismissBankInfoForm() },
                    onSave = { bankName, iban, bic -> walletViewModel.saveBankInfo(bankName, iban, bic) }
                )
            }
        }
}

@Composable
private fun WalletCard(balance: Long) {
    val balanceEuros = balance / 100.0
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFF5745), Color(0xFFEA580C))
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ForkEat Wallet",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "Virtuel",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    Text(
                        text = "Solde disponible",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${DecimalFormat("0.00").format(balanceEuros)} \u20AC",
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActions(
    onRechargeClick: () -> Unit,
    onWithdrawClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Add,
            label = "Recharger",
            subtitle = "Ajouter des fonds",
            onClick = onRechargeClick
        )
        ActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.ArrowDownward,
            label = "Retirer",
            subtitle = "Vers votre compte",
            onClick = onWithdrawClick
        )
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFFFF1F0), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFF5745),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1E293B)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun BankInfoSection(
    bankInfo: fr.uge.android.forkeat.network.dto.wallet.BankInfoResponse?,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = Color(0xFF0F3D3E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Compte bancaire",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1E293B)
                )
            }

            Spacer(Modifier.height(12.dp))

            if (bankInfo != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFDCFCE7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = bankInfo.bankName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Compte enregistre",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onAddClick) {
                    Text("Modifier le compte", color = Color(0xFF0F3D3E))
                }
            } else {
                Text(
                    text = "Aucun compte enregistre. Ajoutez votre IBAN pour pouvoir retirer vos fonds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onAddClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3D3E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ajouter un compte")
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = message, color = Color(0xFF991B1B), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color(0xFF991B1B), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SuccessBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = message, color = Color(0xFF166534), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color(0xFF166534), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun TransactionsSection(transactions: List<TransactionDTO>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = Color(0xFF0F3D3E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Historique des transactions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1E293B)
                )
            }

            if (transactions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        tint = Color(0xFFD1D5DB),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Aucune transaction pour le moment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "Vos transactions apparaitront ici",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )
                }
            } else {
                // Show at most 4 rows at once; scroll for the rest
                Box(modifier = Modifier.heightIn(max = 288.dp)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        transactions.forEach { transaction ->
                            HorizontalDivider(color = Color(0xFFF3F4F6))
                            TransactionRow(transaction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: TransactionDTO) {
    val isCredit = transaction.type == "RECHARGE"
    val iconColor = when (transaction.type) {
        "RECHARGE" -> Color(0xFF16A34A)
        "WITHDRAWAL" -> Color(0xFF3B82F6)
        "SUPER_LIKE" -> Color(0xFFEF4444)
        else -> Color(0xFF8B5CF6)
    }
    val iconBg = when (transaction.type) {
        "RECHARGE" -> Color(0xFFF0FDF4)
        "WITHDRAWAL" -> Color(0xFFEFF6FF)
        "SUPER_LIKE" -> Color(0xFFFEF2F2)
        else -> Color(0xFFF5F3FF)
    }
    val icon = when (transaction.type) {
        "RECHARGE" -> Icons.Default.ArrowUpward
        "WITHDRAWAL" -> Icons.Default.AccountBalance
        "SUPER_LIKE" -> Icons.Default.Favorite
        else -> Icons.Default.SwapHoriz
    }
    val label = when (transaction.type) {
        "RECHARGE" -> "Rechargement"
        "WITHDRAWAL" -> "Retrait bancaire"
        "SUPER_LIKE" -> "Super Like"
        "REDISTRIBUTION" -> "Redistribution"
        else -> transaction.type
    }
    val amountEuros = transaction.amount / 100.0
    val amountText = "${if (isCredit) "+" else "-"}${DecimalFormat("0.00").format(amountEuros)} \u20AC"
    val amountColor = when (transaction.type) {
        "RECHARGE" -> Color(0xFF16A34A)
        "WITHDRAWAL" -> Color(0xFF3B82F6)
        else -> Color(0xFFEF4444)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = formatDate(transaction.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6B7280)
                )
                if (transaction.type == "WITHDRAWAL" && transaction.status != null) {
                    val statusText = when (transaction.status) {
                        "SUCCEEDED" -> "Vire"
                        "PENDING" -> "En cours"
                        else -> "Echoue"
                    }
                    val statusColor = when (transaction.status) {
                        "SUCCEEDED" -> Color(0xFF16A34A)
                        "PENDING" -> Color(0xFFCA8A04)
                        else -> Color(0xFFEF4444)
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = statusColor
                    )
                }
            }
        }
        Text(
            text = amountText,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = amountColor
        )
    }
}

@Composable
private fun RechargeDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFBF9F4),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFFFF1F0), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFFF5745), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Recharger", fontWeight = FontWeight.Bold, color = Color(0xFF0F3D3E))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Montant (en euros)") },
                    placeholder = { Text("Ex: 10") },
                    suffix = { Text("\u20AC", fontWeight = FontWeight.Bold, color = Color(0xFF0F3D3E)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF5745),
                        cursorColor = Color(0xFFFF5745),
                        focusedLabelColor = Color(0xFFFF5745)
                    )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Min 1\u20AC \u2022 Max 500\u20AC",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6B7280)
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5L, 10L, 20L, 50L).forEach { amount ->
                        val isSelected = amountText == amount.toString()
                        Button(
                            onClick = { amountText = amount.toString() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFFF5745) else Color.White,
                                contentColor = if (isSelected) Color.White else Color(0xFF0F3D3E)
                            ),
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)) else null,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("${amount}\u20AC", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull()
                    if (amount != null && amount in 1..500) {
                        onConfirm(amount)
                    }
                },
                enabled = !isLoading && amountText.toLongOrNull()?.let { it in 1..500 } == true,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5745)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Payer avec Stripe", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = Color(0xFF6B7280))
            }
        }
    )
}

@Composable
private fun WithdrawDialog(
    balanceCents: Long,
    hasBankInfo: Boolean,
    bankName: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    onAddBankInfo: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val maxEuros = balanceCents / 100

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFBF9F4),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFE8F5F5), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF0F3D3E), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Retirer", fontWeight = FontWeight.Bold, color = Color(0xFF0F3D3E))
            }
        },
        text = {
            if (!hasBankInfo) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFFFF1F0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFFFF5745), modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Aucun compte bancaire", fontWeight = FontWeight.Medium, color = Color(0xFF0F3D3E), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ajoutez votre IBAN pour retirer des fonds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5F5)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0F3D3E).copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF0F3D3E).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF0F3D3E), modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(bankName ?: "", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color(0xFF0F3D3E))
                                Text("Compte enregistre", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                        label = { Text("Montant (en euros)") },
                        placeholder = { Text("Ex: 10") },
                        suffix = { Text("\u20AC", fontWeight = FontWeight.Bold, color = Color(0xFF0F3D3E)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0F3D3E),
                            cursorColor = Color(0xFF0F3D3E),
                            focusedLabelColor = Color(0xFF0F3D3E)
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Solde disponible : ${DecimalFormat("0.00").format(balanceCents / 100.0)} \u20AC",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B7280)
                    )
                    Spacer(Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F0)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5745).copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFF5745), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Virement en 1 a 3 jours ouvres.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFF5745)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!hasBankInfo) {
                Button(
                    onClick = onAddBankInfo,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3D3E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ajouter un compte", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = {
                        val amount = amountText.toLongOrNull()
                        if (amount != null && amount in 1..maxEuros) {
                            onConfirm(amount)
                        }
                    },
                    enabled = !isLoading && amountText.toLongOrNull()?.let { it in 1..maxEuros } == true,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3D3E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Retirer", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = Color(0xFF6B7280))
            }
        }
    )
}

@Composable
private fun BankInfoFormDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var bankName by remember { mutableStateOf("") }
    var iban by remember { mutableStateOf("") }
    var bic by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFBF9F4),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFE8F5F5), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF0F3D3E), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Compte bancaire", fontWeight = FontWeight.Bold, color = Color(0xFF0F3D3E))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F0)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5745).copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFF5745), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Coordonnees transmises de facon securisee.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF5745)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Nom du titulaire") },
                    placeholder = { Text("Ex: Jean Dupont") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0F3D3E),
                        cursorColor = Color(0xFF0F3D3E),
                        focusedLabelColor = Color(0xFF0F3D3E)
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = iban,
                    onValueChange = { iban = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                    label = { Text("IBAN") },
                    placeholder = { Text("FR7630006000011234567890189") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0F3D3E),
                        cursorColor = Color(0xFF0F3D3E),
                        focusedLabelColor = Color(0xFF0F3D3E)
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text("Sans espaces, en majuscules", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = bic,
                    onValueChange = { bic = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                    label = { Text("BIC / SWIFT") },
                    placeholder = { Text("BNPAFRPPXXX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0F3D3E),
                        cursorColor = Color(0xFF0F3D3E),
                        focusedLabelColor = Color(0xFF0F3D3E)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(bankName, iban, bic) },
                enabled = !isLoading && bankName.isNotBlank() && iban.length >= 15 && bic.length >= 8,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3D3E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Enregistrer", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = Color(0xFF6B7280))
            }
        }
    )
}

private fun formatDate(isoDate: String): String {
    return try {
        val parts = isoDate.take(16).split("T")
        if (parts.size == 2) {
            val dateParts = parts[0].split("-")
            if (dateParts.size == 3) {
                "${dateParts[2]}/${dateParts[1]}/${dateParts[0]} ${parts[1]}"
            } else isoDate
        } else isoDate
    } catch (_: Exception) {
        isoDate
    }
}

private fun openCustomTab(context: Context, url: String) {
    val intent = CustomTabsIntent.Builder().build()
    intent.launchUrl(context, Uri.parse(url))
}
