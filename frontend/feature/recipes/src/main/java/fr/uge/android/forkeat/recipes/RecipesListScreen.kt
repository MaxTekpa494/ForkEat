package fr.uge.android.forkeat.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.uge.android.forkeat.designsystem.theme.Secondary900
import fr.uge.android.forkeat.designsystem.theme.SurfaceCream
import fr.uge.android.forkeat.home.ForkEatScaffold
import fr.uge.android.forkeat.recipes.data.dto.RecipeDTO
import java.util.UUID
import kotlin.time.Instant
import androidx.navigation.NavHostController
import fr.uge.android.forkeat.designsystem.theme.Typography

@Composable
fun RecipesListScreen(
  recipes: List<RecipeDTO>,
  totalCount: Int,
  onLoadMore: () -> Unit,
  errorMessage: String? = null,
  isLoading : Boolean = false,
  navController: NavHostController? = null,
  isLoggedIn: Boolean = false,
  onLogout: () -> Unit = {},
  searchQuery: String = "",
  onSearchQueryChange: (String) -> Unit = {},
  onSearchSubmit: () -> Unit = {},
  availableAllergens: List<String> = emptyList(),
  selectedAllergens: Set<String> = emptySet(),
  onAllergenToggle: (String) -> Unit = {},
  onClearFilters: () -> Unit = {}
) {
  val listState = rememberLazyListState()

  val shouldLoadMore by remember {
    derivedStateOf {
      val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
      lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 1
    }
  }
  LaunchedEffect(shouldLoadMore, recipes.size, totalCount) {
    if (shouldLoadMore) {
      onLoadMore()
    }
  }
  ForkEatScaffold(
    navController = navController,
    isLoggedIn = isLoggedIn,
    onLogout = onLogout,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(SurfaceCream)
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
      Text("Recettes (${recipes.size}/$totalCount)", style = MaterialTheme.typography.titleLarge)
      Spacer(modifier = Modifier.height(8.dp))
      LazyColumn(
        modifier = Modifier.weight(1f),
        state = listState
      ) {
        items(recipes) { recipe ->
          RecipeCard(recipe = recipe, onClick = {
            navController?.navigate("recipes/${recipe.id}")
          })
        }
        if(isLoading) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Center
            ) {
              CircularProgressIndicator()
            }
          }
        }
        // Affichage du message de fin
        if (recipes.size >= totalCount && totalCount > 0) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Center
            ) {
              Text(
                "Fin",
                style = Typography.labelLarge,
                color = Secondary900 // Couleur du design system (onSurface = Secondary900)
              )
            }
          }
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
    onLoadMore = {},
    errorMessage = null
  )
}
