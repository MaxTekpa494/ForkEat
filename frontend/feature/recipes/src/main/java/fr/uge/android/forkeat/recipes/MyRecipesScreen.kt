package fr.uge.android.forkeat.recipes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import fr.uge.android.forkeat.designsystem.theme.*
import fr.uge.android.forkeat.recipes.data.dto.AuthorRecipeSummaryDTO
import fr.uge.android.forkeat.recipes.data.dto.UserRecipeStatsDTO

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        diff < 1 * 60 * 1000 -> "À l'instant"
        diff < 60 * 60 * 1000 -> "Il y a ${diff / 60000} minute(s)"
        diff < 24 * 60 * 60 * 1000 -> "Il y a ${diff / 3600000} heure(s)"
        diff < 30L * 24 * 60 * 60 * 1000 -> "Il y a ${diff / 86400000} jour(s)"
        diff < 12L * 30 * 24 * 60 * 60 * 1000 -> "Il y a ${diff / 2592000000L} mois"
        else -> "Il y a ${diff / 31536000000L} an(s)"
    }
}

private data class StatusTab(
    val status: String,
    val label: String,
    val count: Long
)

@Composable
fun MyRecipesScreen(
    viewModel: MyRecipesViewModel = viewModel(),
    isLoggedIn: Boolean = true,
    onLogout: () -> Unit = {},
    onNavigateToCreateRecipe: () -> Unit = {},
    onNavigateToEdit: (String) -> Unit = {},
    onNavigateToVariant: (String) -> Unit = {},
    onNavigateToDetail: (java.util.UUID) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is MyRecipesNavigationEvent.NavigateToEdit ->
                    onNavigateToEdit(event.recipeId.toString())
                is MyRecipesNavigationEvent.NavigateToVariant ->
                    onNavigateToVariant(event.parentId.toString())
            }
        }
    }

    // Détection de la fin de liste pour la pagination
    val shouldLoadNextPage by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            lastVisibleItemIndex > (totalItemsNumber - 5) && totalItemsNumber > 0
        }
    }

    LaunchedEffect(shouldLoadNextPage) {
        if (shouldLoadNextPage) {
            viewModel.loadNextPage()
        }
    }

    val tabs = listOf(
        StatusTab("PUBLISHED", "Publiées", uiState.stats.published),
        StatusTab("DRAFT", "Brouillons", uiState.stats.draft),
        StatusTab("PENDING_REVIEW", "En attente", uiState.stats.pendingReview),
        StatusTab("REJECTED", "Rejetées", uiState.stats.rejected)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceCream)
                .padding(16.dp)
        ) {
            Text(
                text = "Mes recettes",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Onglets de statut
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOfFirst { it.status == uiState.selectedStatus }.coerceAtLeast(0),
                containerColor = Color.White,
                contentColor = Primary500,
                edgePadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                tabs.forEach { tab ->
                    val selected = tab.status == uiState.selectedStatus
                    Tab(
                        selected = selected,
                        onClick = { viewModel.onStatusChange(tab.status) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    tab.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                if (tab.count > 0) {
                                    Badge(
                                        containerColor = if (selected) Primary500 else Gray100,
                                        contentColor = if (selected) Color.White else Gray500
                                    ) {
                                        Text("${tab.count}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            uiState.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (uiState.isLoading && uiState.recipes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.recipes.isEmpty()) {
                EmptyMyRecipes(
                    status = uiState.selectedStatus,
                    onNavigateToCreateRecipe = onNavigateToCreateRecipe
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState
                ) {
                    items(uiState.recipes) { recipe ->
                        MyRecipeCard(
                            recipe = recipe,
                            onViewDetail = { onNavigateToDetail(recipe.summary.id) },
                            onEdit = { viewModel.onEditRecipe(recipe) },
                            onCreateVariant = { viewModel.onCreateVariant(recipe) },
                            onDelete = { viewModel.requestDelete(recipe) }
                        )
                    }
                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onNavigateToCreateRecipe,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Primary500,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Créer une recette")
        }
    }

    uiState.recipeToDelete?.let { recipe ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Supprimer la recette") },
            text = { Text("Voulez-vous vraiment supprimer « ${recipe.summary.title} » ? Cette action est irréversible.") },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun EmptyMyRecipes(status: String, onNavigateToCreateRecipe: () -> Unit) {
    val message = when (status) {
        "DRAFT" -> "Aucun brouillon."
        "PENDING_REVIEW" -> "Aucune recette en attente de validation."
        "REJECTED" -> "Aucune recette rejetée."
        else -> "Vous n'avez pas encore de recettes publiées."
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            if (status == "PUBLISHED" || status == "DRAFT") {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateToCreateRecipe,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary500)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Créer ma première recette", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun MyRecipeCard(
    recipe: AuthorRecipeSummaryDTO,
    onViewDetail: () -> Unit,
    onEdit: () -> Unit,
    onCreateVariant: () -> Unit,
    onDelete: () -> Unit
) {
    val isPublished = recipe.status == "PUBLISHED"
    val isRejected = recipe.status == "REJECTED"
    var showJustification by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onViewDetail() },
            shape = RoundedCornerShape(24.dp),
            colors = cardColors(containerColor = Color.White),
            elevation = cardElevation(defaultElevation = 1.dp),
            border = BorderStroke(1.dp, if (isRejected) Color(0xFFFEE2E2) else Gray100)
        ) {
            val timeLabel = remember(recipe.summary.createdAt) {
                recipe.summary.createdAt.toEpochMilliseconds().toRelativeTime()
            }
            Column {
                Row(modifier = Modifier.padding(8.dp)) {
                    Image(
                        painter = rememberAsyncImagePainter(recipe.summary.imageUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .aspectRatio(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                recipe.summary.title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Secondary700,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            StatusBadge(status = recipe.status)
                        }
                        Text(
                            recipe.summary.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = Gray500
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Créée $timeLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gray500
                        )
                    }
                }

                val rejectionInfo = recipe.rejectionInfo
                if (isRejected && rejectionInfo != null) {
                    Divider(color = Color(0xFFFEE2E2), modifier = Modifier.padding(horizontal = 16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2))
                            .clickable { showJustification = !showJustification }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Voir la justification du rejet",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFB91C1C)
                            )
                        }
                        AnimatedVisibility(visible = showJustification) {
                            Text(
                                text = rejectionInfo.justification,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.padding(top = 4.dp, start = 24.dp)
                            )
                        }
                    }
                }
                
                Divider(color = Gray100, modifier = Modifier.padding(horizontal = 16.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isPublished) {
                        IconButton(onClick = onCreateVariant) {
                            Icon(Icons.Default.AltRoute, contentDescription = "Créer une variante", tint = Primary500)
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = Primary500)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (text, color) = when (status) {
        "PUBLISHED" -> "Publié" to Color(0xFF10B981)
        "DRAFT" -> "Brouillon" to Color(0xFF6B7280)
        "PENDING_REVIEW" -> "En attente" to Color(0xFFF59E0B)
        "REJECTED" -> "Rejeté" to Color(0xFFEF4444)
        else -> status to Color.Gray
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
