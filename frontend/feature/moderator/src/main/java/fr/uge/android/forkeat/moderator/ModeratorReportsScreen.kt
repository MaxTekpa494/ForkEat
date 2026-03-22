package fr.uge.android.forkeat.moderator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import fr.uge.android.forkeat.designsystem.InfiniteListHandler
import fr.uge.android.forkeat.moderator.data.dto.UserModerationRequest
import java.util.UUID

private val AdminPurple700 = Color(0xFF5B21B6)

// Data class pour les actions utilisateur (hors du composable)
data class UserReportActionData(val reportId: UUID, val userId: UUID)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeratorReportsScreen(
    currentRoute: String,
    onNavigateToRecipes: () -> Unit,
    onExit: () -> Unit,
    viewModel: RecipeReportsViewModel = viewModel(),
    userReportsViewModel: UserReportsViewModel = viewModel(),
    onNavigateToRecipe: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    adminTheme: Boolean = false,
) {
    val errorColor = if (adminTheme) Color(0xFFB91C1C) else Color(0xFFDC2626)
    val cardColor = if (adminTheme) Color(0xFFF3F0FF) else Color(0xFFFFF7ED)
    val buttonReject = if (adminTheme) AdminPurple700 else Color(0xFF16A34A)
    val buttonValidate = if (adminTheme) errorColor else Color(0xFFDC2626)
    val tabBarColor = if (adminTheme) AdminPurple700 else Color(0xFFEA580C)

    var selectedTab by remember { mutableIntStateOf(0) }
    val recipeReportsCount = viewModel.uiState.collectAsState().value.reports.size
    val userReportsState by userReportsViewModel.uiState.collectAsState()
    val userReportsCount = userReportsState.reports.size
    val tabs = listOf(
        "Signalements recettes ($recipeReportsCount)",
        "Signalements utilisateurs ($userReportsCount)"
    )
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val userListState = rememberLazyListState()
    var showDismissDialog by remember { mutableStateOf<Pair<UUID, UUID>?>(null) }
    var dismissJustification by remember { mutableStateOf("") }
    // Pour chaque action utilisateur, on a une modale dédiée
    var showRejectDialog by remember { mutableStateOf<UserReportActionData?>(null) }
    var showWarnDialog by remember { mutableStateOf<UserReportActionData?>(null) }
    var showSuspendDialog by remember { mutableStateOf<UserReportActionData?>(null) }
    var showBanDialog by remember { mutableStateOf<UserReportActionData?>(null) }
    var actionJustification by remember { mutableStateOf("") }
    var suspensionDays by remember { mutableIntStateOf(0) }
    var suspensionHours by remember { mutableIntStateOf(0) }

    ModeratorScaffold(
        currentRoute = currentRoute,
        onNavigateToRecipes = onNavigateToRecipes,
        onNavigateToReports = {},
        onExitToNonModerator = onExit
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                contentColor = tabBarColor,
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTab, matchContentSize = true),
                        color = tabBarColor
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Bandeau succès/erreur recettes
            if (selectedTab == 0) {
                if (uiState.successMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(Color(0xFFDCFCE7), shape = RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(text = uiState.successMessage!!, color = Color(0xFF166534), fontSize = 13.sp)
                        }
                        TextButton(onClick = { viewModel.clearMessage() }) {
                            Text("OK", color = Color(0xFF166534), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                if (uiState.error != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(Color(0xFFFEF2F2), shape = RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = uiState.error!!, color = Color(0xFFDC2626), fontSize = 13.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearMessage() }) {
                            Text("OK", color = Color(0xFFDC2626), fontSize = 12.sp)
                        }
                    }
                }
            } else {
                if (userReportsState.successMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(Color(0xFFDCFCE7), shape = RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(text = userReportsState.successMessage!!, color = Color(0xFF166534), fontSize = 13.sp)
                        }
                        TextButton(onClick = { userReportsViewModel.clearMessage() }) {
                            Text("OK", color = Color(0xFF166534), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                if (userReportsState.error != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(Color(0xFFFEF2F2), shape = RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = userReportsState.error!!, color = Color(0xFFDC2626), fontSize = 13.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { userReportsViewModel.clearMessage() }) {
                            Text("OK", color = Color(0xFFDC2626), fontSize = 12.sp)
                        }
                    }
                }
            }
            when (selectedTab) {
                0 -> {
                    // Liste des signalements de recettes
                    Box(Modifier.fillMaxSize()) {
                        if (uiState.isLoading && uiState.reports.isEmpty()) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        } else if (uiState.reports.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(56.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("Aucun signalement de recette", color = Color(0xFF6B7280), fontSize = 15.sp)
                                    Text("Tout est à jour !", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(uiState.reports) { report ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToRecipe(report.recipeId.toString()) },
                                        colors = CardDefaults.cardColors(containerColor = cardColor)
                                    ) {
                                        Row(
                                            Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(report.recipeTitle, fontWeight = FontWeight.Bold)
                                                Text("Type: ${report.reportType}", color = Color(0xFFDC2626))
                                                Text("Signalé par: ${report.reporterUsername}")
                                                Text("Justification: ${report.justification}")
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Column(horizontalAlignment = Alignment.End) {
                                                Button(
                                                    onClick = { viewModel.dismissReport(report.id, report.recipeId, "Justifié") },
                                                    enabled = uiState.actionInProgress != report.id,
                                                    colors = ButtonDefaults.buttonColors(containerColor = buttonReject)
                                                ) { Text("Rejeter", color = Color.White) }
                                                Spacer(Modifier.height(4.dp))
                                                OutlinedButton(
                                                    onClick = { viewModel.validateReport(report.id, report.recipeId) },
                                                    enabled = uiState.actionInProgress != report.id,
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = buttonValidate)
                                                ) { Text("Valider", color = buttonValidate) }
                                            }
                                        }
                                    }
                                }
                            }
                            InfiniteListHandler(
                                listState = listState,
                                isLoading = uiState.isLoading,
                                buffer = 1,
                                onLoadMore = { viewModel.loadMoreReports() }
                            )
                        }
                        if (showDismissDialog != null) {
                            AlertDialog(
                                onDismissRequest = { showDismissDialog = null },
                                title = { Text("Rejeter le signalement") },
                                text = {
                                    OutlinedTextField(
                                        value = dismissJustification,
                                        onValueChange = { dismissJustification = it },
                                        label = { Text("Justification") }
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            val (reportId, recipeId) = showDismissDialog!!
                                            viewModel.dismissReport(reportId, recipeId, dismissJustification)
                                            showDismissDialog = null
                                            dismissJustification = ""
                                        },
                                        enabled = dismissJustification.isNotBlank()
                                    ) { Text("Rejeter") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDismissDialog = null }) { Text("Annuler") }
                                }
                            )
                        }
                    }
                }
                1 -> {
                    // Liste des signalements utilisateurs
                    Box(Modifier.fillMaxSize()) {
                        if (userReportsState.isLoading && userReportsState.reports.isEmpty()) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        } else if (userReportsState.reports.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(56.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("Aucun signalement utilisateur", color = Color(0xFF6B7280), fontSize = 15.sp)
                                    Text("Tout est à jour !", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                state = userListState,
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(userReportsState.reports) { report ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToUserProfile(report.reportedUsername) },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                                    ) {
                                        Row(
                                            Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(20.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(report.reportedUsername, fontWeight = FontWeight.Bold)
                                                }
                                                Text("Type: ${report.reportType}", color = Color(0xFFDC2626))
                                                Text("Signalé par: ${report.reporterUsername}")
                                                Text("Justification: ${report.justification}")
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Column(horizontalAlignment = Alignment.End) {
                                                Button(
                                                    onClick = { showRejectDialog = UserReportActionData(report.id, report.reportedUserId); actionJustification = "" },
                                                    enabled = userReportsState.actionInProgress != report.id,
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9CA3AF))
                                                ) { Text("Rejeter", color = Color.White) }
                                                Spacer(Modifier.height(4.dp))
                                                Button(
                                                    onClick = { showWarnDialog = UserReportActionData(report.id, report.reportedUserId); actionJustification = "" },
                                                    enabled = userReportsState.actionInProgress != report.id,
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFACC15))
                                                ) { Row { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309)); Spacer(Modifier.width(4.dp)); Text("Avertir", color = Color(0xFFB45309)) } }
                                                Spacer(Modifier.height(4.dp))
                                                Button(
                                                    onClick = { showSuspendDialog = UserReportActionData(report.id, report.reportedUserId); actionJustification = ""; suspensionDays = 0; suspensionHours = 0 },
                                                    enabled = userReportsState.actionInProgress != report.id,
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
                                                ) { Row { Icon(Icons.Default.PauseCircle, contentDescription = null, tint = Color(0xFFB45309)); Spacer(Modifier.width(4.dp)); Text("Suspendre", color = Color.White) } }
                                                Spacer(Modifier.height(4.dp))
                                                Button(
                                                    onClick = { showBanDialog = UserReportActionData(report.id, report.reportedUserId); actionJustification = "" },
                                                    enabled = userReportsState.actionInProgress != report.id,
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                                ) { Row { Icon(Icons.Default.Block, contentDescription = null, tint = Color.White); Spacer(Modifier.width(4.dp)); Text("Bannir", color = Color.White) } }
                                            }
                                        }
                                    }
                                }
                            }
                            InfiniteListHandler(
                                listState = userListState,
                                isLoading = userReportsState.isLoading,
                                buffer = 1,
                                onLoadMore = { userReportsViewModel.loadMoreReports() }
                            )
                        }
                        // Dialog pour rejeter
                        if (showRejectDialog != null) {
                            AlertDialog(
                                onDismissRequest = { showRejectDialog = null },
                                title = { Text("Rejeter le signalement utilisateur") },
                                text = {
                                    OutlinedTextField(
                                        value = actionJustification,
                                        onValueChange = { actionJustification = it },
                                        label = { Text("Justification") }
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            val (reportId, userId) = showRejectDialog!!
                                            userReportsViewModel.resolveUserReport(
                                                reportId,
                                                UserModerationRequest("DISMISSED", actionJustification, userId, 0, 0)
                                            )
                                            showRejectDialog = null
                                            actionJustification = ""
                                        },
                                        enabled = actionJustification.isNotBlank()
                                    ) { Text("Rejeter") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showRejectDialog = null }) { Text("Annuler") }
                                }
                            )
                        }
                        // Dialog pour avertir
                        if (showWarnDialog != null) {
                            AlertDialog(
                                onDismissRequest = { showWarnDialog = null },
                                title = { Text("Avertir l'utilisateur") },
                                text = {
                                    OutlinedTextField(
                                        value = actionJustification,
                                        onValueChange = { actionJustification = it },
                                        label = { Text("Justification") }
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            val (reportId, userId) = showWarnDialog!!
                                            userReportsViewModel.resolveUserReport(
                                                reportId,
                                                UserModerationRequest("WARNING", actionJustification, userId, 0, 0)
                                            )
                                            showWarnDialog = null
                                            actionJustification = ""
                                        },
                                        enabled = actionJustification.isNotBlank()
                                    ) { Text("Avertir") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showWarnDialog = null }) { Text("Annuler") }
                                }
                            )
                        }
                        // Dialog pour suspendre
                        if (showSuspendDialog != null) {
                            AlertDialog(
                                onDismissRequest = { showSuspendDialog = null },
                                title = { Text("Suspendre l'utilisateur") },
                                text = {
                                    Column {
                                        OutlinedTextField(
                                            value = actionJustification,
                                            onValueChange = { actionJustification = it },
                                            label = { Text("Justification") }
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Jours : ")
                                            OutlinedTextField(
                                                value = suspensionDays.toString(),
                                                onValueChange = { v -> suspensionDays = v.toIntOrNull() ?: 0 },
                                                modifier = Modifier.width(60.dp),
                                                singleLine = true
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text("Heures : ")
                                            OutlinedTextField(
                                                value = suspensionHours.toString(),
                                                onValueChange = { v -> suspensionHours = v.toIntOrNull() ?: 0 },
                                                modifier = Modifier.width(60.dp),
                                                singleLine = true
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            val (reportId, userId) = showSuspendDialog!!
                                            userReportsViewModel.resolveUserReport(
                                                reportId,
                                                UserModerationRequest("SUSPENDED", actionJustification, userId, suspensionDays, suspensionHours)
                                            )
                                            showSuspendDialog = null
                                            actionJustification = ""
                                            suspensionDays = 0
                                            suspensionHours = 0
                                        },
                                        enabled = actionJustification.isNotBlank() && (suspensionDays > 0 || suspensionHours > 0)
                                    ) { Text("Suspendre") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showSuspendDialog = null }) { Text("Annuler") }
                                }
                            )
                        }
                        // Dialog pour bannir
                        if (showBanDialog != null) {
                            AlertDialog(
                                onDismissRequest = { showBanDialog = null },
                                title = { Text("Bannir l'utilisateur") },
                                text = {
                                    OutlinedTextField(
                                        value = actionJustification,
                                        onValueChange = { actionJustification = it },
                                        label = { Text("Justification") }
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            val (reportId, userId) = showBanDialog!!
                                            userReportsViewModel.resolveUserReport(
                                                reportId,
                                                UserModerationRequest("BANNED", actionJustification, userId, 0, 0)
                                            )
                                            showBanDialog = null
                                            actionJustification = ""
                                        },
                                        enabled = actionJustification.isNotBlank()
                                    ) { Text("Bannir") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showBanDialog = null }) { Text("Annuler") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
