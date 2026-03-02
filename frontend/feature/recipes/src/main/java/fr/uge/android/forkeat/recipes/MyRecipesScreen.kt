package fr.uge.android.forkeat.recipes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import fr.uge.android.forkeat.designsystem.theme.*
import fr.uge.android.forkeat.recipes.data.dto.RecipeDTO

// Fonction utilitaire pour convertir un timestamp en une chaîne de temps relatif
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

@Composable
fun RecipeCard(recipe: RecipeDTO, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(24.dp),
        colors = cardColors(containerColor = Color.White),
        elevation = cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Gray100)
    ) {
        val timeLabel = remember(recipe.createdAt) {
            recipe.createdAt.toEpochMilliseconds().toRelativeTime()
        }

        Row(modifier = Modifier.padding(8.dp)) {
            // Image
            Image(
                painter = rememberAsyncImagePainter(recipe.imageUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .aspectRatio(1f)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Titre
                Text(
                    recipe.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Secondary700
                )
                // Description
                Text(
                    recipe.summary,
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
                // Auteur
                Text(
                    "Par ${recipe.username}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Secondary700
                )
                // Temps de préparation en bas à droite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "${recipe.preparationMinutes} min",
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary500
                    )
                }
            }
        }
    }
}

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceCream)
                .padding(16.dp)
        ) {
            Text(
                text = "Mes recettes (${uiState.recipes.size})",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            uiState.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.recipes.isEmpty()) {
                EmptyMyRecipes(onNavigateToCreateRecipe = onNavigateToCreateRecipe)
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.recipes) { recipe ->
                        MyRecipeCard(
                            recipe = recipe,
                            onViewDetail = { onNavigateToDetail(recipe.id) },
                            onEdit = { viewModel.onEditRecipe(recipe) },
                            onCreateVariant = { viewModel.onCreateVariant(recipe) },
                            onDelete = { viewModel.requestDelete(recipe) }
                        )
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
            text = { Text("Voulez-vous vraiment supprimer « ${recipe.title} » ? Cette action est irréversible.") },
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
private fun EmptyMyRecipes(onNavigateToCreateRecipe: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Vous n'avez pas encore de recettes.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
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

@Composable
private fun MyRecipeCard(
    recipe: RecipeDTO,
    onViewDetail: () -> Unit,
    onEdit: () -> Unit,
    onCreateVariant: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        RecipeCard(recipe = recipe, onClick = onViewDetail)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCreateVariant) {
                Icon(
                    imageVector = Icons.Default.AltRoute,
                    contentDescription = "Créer une variante",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Modifier",
                    tint = Primary500,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
