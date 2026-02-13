package fr.uge.android.forkeat.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.ButtonDefaults.buttonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import fr.uge.android.forkeat.designsystem.theme.Gray500
import fr.uge.android.forkeat.designsystem.theme.Primary500
import fr.uge.android.forkeat.designsystem.theme.Secondary700
import fr.uge.android.forkeat.designsystem.theme.SurfaceCream
import fr.uge.android.forkeat.designsystem.theme.Typography
import fr.uge.android.forkeat.home.ForkEatScaffold
import fr.uge.android.forkeat.recipes.data.dto.RecipeDTO
import java.util.UUID
import kotlin.time.Instant

@Composable
fun RecipesListScreen(
  recipes: List<RecipeDTO>,
  totalCount: Int,
  currentPage: Int,
  pageSize: Int,
  onPageChange: (Int) -> Unit,
  errorMessage: String? = null,
  navController: NavHostController? = null,
  searchQuery: String = "",
  onSearchQueryChange: (String) -> Unit = {},
  onSearchSubmit: () -> Unit = {},
  availableAllergens: List<String> = emptyList(),
  selectedAllergens: Set<String> = emptySet(),
  onAllergenToggle: (String) -> Unit = {},
  onClearFilters: () -> Unit = {}
) {
  ForkEatScaffold(
    navController = navController,
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(SurfaceCream)
        .padding(paddingValues)
        .padding(16.dp)
    ) {
      RecipeSearchFilterBar(
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onSearchSubmit = onSearchSubmit,
        availableAllergens = availableAllergens,
        selectedAllergens = selectedAllergens,
        onAllergenToggle = onAllergenToggle,
        onClearFilters = onClearFilters
      )
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
          RecipeCard(recipe = recipe, onClick = {
            navController?.navigate("recipes/${recipe.id}")
          })
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      val start = currentPage * pageSize + 1
      val end = minOf((currentPage + 1) * pageSize, totalCount)
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = SpaceBetween) {
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
      createdAt = Instant.parse("2026-02-09T12:00:00Z"),
      updatedAt = Instant.parse("2026-02-09T12:00:00Z")
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
      createdAt = Instant.parse("2026-02-09T12:00:00Z"),
      updatedAt = Instant.parse("2026-02-09T12:00:00Z")
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
