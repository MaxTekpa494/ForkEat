package fr.uge.android.forkeat.recipes

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.uge.android.forkeat.designsystem.theme.Primary500
import fr.uge.android.forkeat.designsystem.theme.Primary100
import fr.uge.android.forkeat.designsystem.theme.Secondary700
import fr.uge.android.forkeat.designsystem.theme.Gray100
import fr.uge.android.forkeat.designsystem.theme.Gray500
import fr.uge.android.forkeat.designsystem.theme.Typography
import fr.uge.android.forkeat.home.ForkEatScaffold
import fr.uge.android.forkeat.recipes.data.dto.RecipeDTO
import java.util.UUID
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.ButtonDefaults.buttonElevation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import fr.uge.android.forkeat.designsystem.theme.SurfaceCream
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.navigation.NavHostController
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField

@Composable
fun RecipesListScreen(
    recipes: List<RecipeDTO>,
    totalCount: Int,
    currentPage: Int,
    pageSize: Int,
    onPageChange: (Int) -> Unit,
    errorMessage: String? = null,
    navController: NavHostController? = null,
    onSearch: ((String) -> Unit)? = null
) {
    var selectedRecipe by remember { mutableStateOf<RecipeDTO?>(null) }
    var showSearchField by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    ForkEatScaffold(
        onNavigateToLogin = {},
        showSearchIcon = true,
        onSearchIconClick = { showSearchField = !showSearchField },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceCream)
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                if (showSearchField) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = {
                            searchText = it
                            onSearch?.invoke(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        placeholder = { Text("Rechercher une recette...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            focusedBorderColor = Primary500,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                        ),
                        singleLine = true
                    )
                }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = Red,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Text("Recettes ($totalCount)", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(recipes) { recipe ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    navController?.navigate("recipes/${recipe.id}")
                                },
                            shape = RoundedCornerShape(24.dp),
                            colors = cardColors(containerColor = Color.White),
                            elevation = cardElevation(defaultElevation = 1.dp),
                            border = BorderStroke(1.dp, Gray100)
                        ) {
                            Row(modifier = Modifier.padding(8.dp)) {
                                // Image
                                Image(
                                    painter = rememberAsyncImagePainter(recipe.imageUrl),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .aspectRatio(1f)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    // Titre + infos
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = SpaceBetween
                                    ) {
                                        Text(
                                            recipe.title,
                                            style = Typography.titleLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = Secondary700
                                        )
                                    }
                                    // Description
                                    Text(
                                        recipe.summary,
                                        style = Typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Gray500
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    // Auteur
                                    Text(
                                        "Par ${recipe.username}",
                                        style = Typography.labelSmall,
                                        color = Secondary700
                                    )
                                    // Temps de préparation en bas à droite
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            "${recipe.preparationMinutes} min",
                                            style = Typography.labelMedium,
                                            color = Primary500
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val start = currentPage * pageSize + 1
                val end = minOf((currentPage + 1) * pageSize, totalCount)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(
                        onClick = { onPageChange(currentPage - 1) },
                        enabled = currentPage > 0,
                        shape = RoundedCornerShape(50),
                        colors = buttonColors(
                            containerColor = Primary500,
                            contentColor = Color.White
                        ),
                        elevation = buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Text("Précédent", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Page ${currentPage + 1}", style = Typography.bodyMedium, color = Secondary700)
                        Text("$start-$end sur $totalCount", style = Typography.labelSmall, color = Gray500)
                    }
                    Button(
                        onClick = { onPageChange(currentPage + 1) },
                        enabled = recipes.isNotEmpty(),
                        shape = RoundedCornerShape(50),
                        colors = buttonColors(
                            containerColor = Primary500,
                            contentColor = Color.White
                        ),
                        elevation = buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Text("Suivant", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }
    )
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
fun RecipesListScreenPreview() {
    val sampleRecipes = listOf(
        RecipeDTO(
            id = UUID.randomUUID(),
            title = "Tarte aux pommes",
            summary = "Une délicieuse tarte aux pommes maison.",
            parent = null,
            username = "chef1",
            preparationMinutes = 45,
            imageUrl = "https://cdn.chefclub.tools/uploads/recipes/cover-thumbnail/f1ca20f3-f78f-4d0b-a642-369fbefb05b9_9PNuzny.jpg",
            status = "PUBLISHED",
            steps = emptyList(),
            ingredients = emptyList(),
            allergens = emptyList(),
            dietaryFlags = emptyMap(),
            createdAt = "2026-02-09T12:00:00Z",
            updatedAt = "2026-02-09T12:00:00Z"
        ),
        RecipeDTO(
            id = UUID.randomUUID(),
            title = "Quiche lorraine",
            summary = "La vraie quiche lorraine traditionnelle.",
            parent = null,
            username = "chef2",
            preparationMinutes = 60,
            imageUrl = "https://assets.afcdn.com/recipe/20221010/135915_w1024h768c1cx999cy749cxt0cyt0cxb1999cyb1499.webp",
            status = "PUBLISHED",
            steps = emptyList(),
            ingredients = emptyList(),
            allergens = emptyList(),
            dietaryFlags = emptyMap(),
            createdAt = "2026-02-09T12:00:00Z",
            updatedAt = "2026-02-09T12:00:00Z"
        )
    )
    RecipesListScreen(
        recipes = sampleRecipes,
        totalCount = 2,
        currentPage = 0,
        pageSize = 10,
        onPageChange = {},
        errorMessage = null
    )
}
