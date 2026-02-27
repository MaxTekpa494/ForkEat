package fr.uge.android.forkeat.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import coil.compose.AsyncImage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.uge.android.forkeat.network.dto.admin.AdminRecipeDTO

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRecipesScreen(
    currentRoute: String,
    onNavigateToDashboard: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToWallets: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToRecipe: (String) -> Unit = {},
    onLogout: () -> Unit,
    viewModel: AdminRecipesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "En attente (${uiState.pendingRecipes.size})",
        "Publiées (${uiState.publishedTotal})"
    )

    AdminScaffold(
        currentRoute = currentRoute,
        onNavigateToDashboard = onNavigateToDashboard,
        onNavigateToUsers = onNavigateToUsers,
        onNavigateToRecipes = {},
        onNavigateToWallets = onNavigateToWallets,
        onNavigateToCreate = onNavigateToCreate,
        onLogout = onLogout
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = {
                if (selectedTab == 0) viewModel.loadPending() else viewModel.loadPublished(uiState.publishedPage)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F3FF))
            ) {
                // En-tête avec gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(AdminPurple900, AdminPurple800))
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Column {
                        Text(
                            text = "Recettes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Modération et publication",
                            fontSize = 13.sp,
                            color = AdminPurple300
                        )
                    }
                }

                // Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = AdminPurple900,
                    contentColor = AdminPurple300,
                    edgePadding = 0.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                if (index == 0) viewModel.loadPending()
                                else viewModel.loadPublished(0)
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) AdminPurple300 else Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }

                // Bandeau succès
                if (uiState.successMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(text = uiState.successMessage!!, color = Color(0xFF166534), fontSize = 13.sp)
                        }
                        TextButton(onClick = { viewModel.clearMessage() }) {
                            Text("OK", color = Color(0xFF166534), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Bandeau erreur
                if (uiState.error != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
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

                when (selectedTab) {
                    0 -> PendingRecipesTab(
                        recipes = uiState.pendingRecipes,
                        actionInProgress = uiState.actionInProgress,
                        onValidate = { viewModel.validateRecipe(it) },
                        onReject = { viewModel.rejectRecipe(it) },
                        onNavigateToRecipe = onNavigateToRecipe
                    )
                    1 -> PublishedRecipesTab(
                        recipes = uiState.publishedRecipes,
                        currentPage = uiState.publishedPage,
                        totalPages = if (uiState.publishedTotal == 0L) 0
                                     else ((uiState.publishedTotal - 1) / 20 + 1).toInt(),
                        actionInProgress = uiState.actionInProgress,
                        onPageChange = { viewModel.loadPublished(it) },
                        onNavigateToRecipe = onNavigateToRecipe,
                        onUnpublish = { viewModel.unpublishRecipe(it) }
                    )
                }
            }
        }
    }
}

// ── Onglet recettes en attente ────────────────────────────────────────────────

@Composable
private fun PendingRecipesTab(
    recipes: List<AdminRecipeDTO>,
    actionInProgress: String?,
    onValidate: (String) -> Unit,
    onReject: (String) -> Unit,
    onNavigateToRecipe: (String) -> Unit
) {
    if (recipes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text("Aucune recette en attente", color = Color(0xFF6B7280), fontSize = 15.sp)
                Text("Tout est à jour !", color = Color(0xFF9CA3AF), fontSize = 13.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            items(recipes) { recipe ->
                PendingRecipeCard(
                    recipe = recipe,
                    isActionInProgress = actionInProgress == recipe.id,
                    onValidate = { onValidate(recipe.id) },
                    onReject = { onReject(recipe.id) },
                    onNavigateToRecipe = { onNavigateToRecipe(recipe.id) }
                )
            }
        }
    }
}

@Composable
private fun PendingRecipeCard(
    recipe: AdminRecipeDTO,
    isActionInProgress: Boolean,
    onValidate: () -> Unit,
    onReject: () -> Unit,
    onNavigateToRecipe: () -> Unit
) {
    var showRejectDialog by remember { mutableStateOf(false) }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Rejeter la recette", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Voulez-vous rejeter « ${recipe.title} » ?\nL'auteur sera informé du refus.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRejectDialog = false
                        onReject()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Rejeter", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isActionInProgress) { onNavigateToRecipe() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Badge statut + titre
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("En attente", fontSize = 10.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                }
                if (recipe.summary != null) {
                    Spacer(Modifier.width(6.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = recipe.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1E293B)
            )

            recipe.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = summary,
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 2
                )
            }

            Spacer(Modifier.height(10.dp))

            // Méta-infos
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RecipeMetaChip(icon = Icons.Default.Person, label = "@${recipe.username}")
                RecipeMetaChip(icon = Icons.Default.Schedule, label = "${recipe.preparationMinutes} min")
                if (recipe.ingredients.isNotEmpty()) {
                    RecipeMetaChip(icon = Icons.Default.MenuBook, label = "${recipe.ingredients.size} ing.")
                }
            }

            Spacer(Modifier.height(12.dp))

            // Boutons d'action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showRejectDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isActionInProgress,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626))
                ) {
                    if (isActionInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFFDC2626))
                    } else {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Rejeter", fontSize = 13.sp)
                    }
                }
                Button(
                    onClick = onValidate,
                    modifier = Modifier.weight(1f),
                    enabled = !isActionInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    if (isActionInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Valider", fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

// ── Onglet recettes publiées ──────────────────────────────────────────────────

@Composable
private fun PublishedRecipesTab(
    recipes: List<AdminRecipeDTO>,
    currentPage: Int,
    totalPages: Int,
    actionInProgress: String?,
    onPageChange: (Int) -> Unit,
    onNavigateToRecipe: (String) -> Unit,
    onUnpublish: (String) -> Unit
) {
    if (recipes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = AdminPurple300, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text("Aucune recette publiée", color = Color(0xFF6B7280), fontSize = 15.sp)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(recipes) { recipe ->
                    PublishedRecipeCard(
                        recipe = recipe,
                        isActionInProgress = actionInProgress == recipe.id,
                        onNavigateToRecipe = { onNavigateToRecipe(recipe.id) },
                        onUnpublish = { onUnpublish(recipe.id) }
                    )
                }
            }

            // Pagination
            if (totalPages > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onPageChange(currentPage - 1) },
                        enabled = currentPage > 0
                    ) {
                        Text("Précédent")
                    }
                    Text(
                        text = "${currentPage + 1} / $totalPages",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(
                        onClick = { onPageChange(currentPage + 1) },
                        enabled = currentPage < totalPages - 1
                    ) {
                        Text("Suivant")
                    }
                }
            }
        }
    }
}

@Composable
private fun PublishedRecipeCard(
    recipe: AdminRecipeDTO,
    isActionInProgress: Boolean,
    onNavigateToRecipe: () -> Unit,
    onUnpublish: () -> Unit
) {
    var showUnpublishDialog by remember { mutableStateOf(false) }

    if (showUnpublishDialog) {
        AlertDialog(
            onDismissRequest = { showUnpublishDialog = false },
            title = { Text("Dépublier la recette", fontWeight = FontWeight.Bold) },
            text = { Text("Voulez-vous dépublier « ${recipe.title} » ?", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = { showUnpublishDialog = false; onUnpublish() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("Dépublier", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showUnpublishDialog = false }) { Text("Annuler") }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isActionInProgress) { onNavigateToRecipe() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Image ou placeholder
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(AdminPurple100, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!recipe.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = recipe.imageUrl,
                            contentDescription = recipe.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = AdminPurple500, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = recipe.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1E293B), maxLines = 2)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "@${recipe.username}", fontSize = 11.sp, color = Color(0xFF6B7280))
                        Text(text = "${recipe.preparationMinutes} min", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                    }
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFDCFCE7), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("Publiée", fontSize = 10.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Bouton dépublier
            OutlinedButton(
                onClick = { showUnpublishDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isActionInProgress,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626))
            ) {
                if (isActionInProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFFDC2626))
                } else {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Dépublier", fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Utilitaire ────────────────────────────────────────────────────────────────

@Composable
private fun RecipeMetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(3.dp))
        Text(text = label, fontSize = 11.sp, color = Color(0xFF6B7280))
    }
}
