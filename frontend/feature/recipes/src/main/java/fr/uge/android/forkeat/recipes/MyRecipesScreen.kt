package fr.uge.android.forkeat.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.uge.android.forkeat.designsystem.theme.Primary500
import fr.uge.android.forkeat.designsystem.theme.SurfaceCream
import fr.uge.android.forkeat.recipes.data.dto.RecipeDTO

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
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
