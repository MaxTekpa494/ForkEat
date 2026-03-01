package fr.uge.android.forkeat.recipes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import fr.uge.android.forkeat.network.ForkEatApi
import java.util.UUID

fun NavGraphBuilder.recipesGraph(
    navController: NavController,
    isLoggedIn: Boolean,
    onLogout: () -> Unit
) {
    composable("recipes") {
        val recipesViewModel: RecipesViewModel = viewModel()
        val recipes by recipesViewModel.recipes.collectAsState()
        val totalCount by recipesViewModel.totalCount.collectAsState()
        val errorMessage by recipesViewModel.errorMessage.collectAsState()
        val searchQuery by recipesViewModel.searchQuery.collectAsState()
        val selectedAllergens by recipesViewModel.selectedAllergens.collectAsState()
        val availableAllergens by recipesViewModel.availableAllergens.collectAsState()
        val isLoading by recipesViewModel.isLoading.collectAsState()

        RecipesListScreen(
            recipes = recipes,
            totalCount = totalCount,
            onLoadMore = { recipesViewModel.loadMoreRecipes() },
            errorMessage = errorMessage,
            isLoading = isLoading,
            isLoggedIn = isLoggedIn,
            onNavigateToCreateRecipe = { navController.navigate("recipe-form") },
            searchQuery = searchQuery,
            onSearchQueryChange = { query -> recipesViewModel.onSearchQueryChange(query) },
            onSearchSubmit = { recipesViewModel.onSearchSubmit() },
            availableAllergens = availableAllergens,
            selectedAllergens = selectedAllergens,
            onAllergenToggle = { allergen -> recipesViewModel.toggleAllergen(allergen) },
            onClearFilters = { recipesViewModel.clearFilters() },
            onRecipeClick = { id -> navController.navigate("recipes/$id") },
        )
    }

    composable(
        route = "recipe-form?recipeId={recipeId}&parentId={parentId}",
        arguments = listOf(
            navArgument("recipeId") { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("parentId") { type = NavType.StringType; nullable = true; defaultValue = null }
        )
    ) {
        val formViewModel: RecipeFormViewModel = viewModel()
        RecipeFormScreen(
            viewModel = formViewModel,
            onBack = { navController.popBackStack() },
            onSuccess = {
                // Note: If we want to refresh the list, we might need a shared event or 
                // just rely on the fact that the ListScreen will reload on resume if needed.
                // Or use a shared ViewModel if absolutely necessary, but here we'll try to keep it isolated.
                navController.navigate("recipes") {
                    popUpTo("recipe-form") { inclusive = true }
                }
            }
        )
    }

    composable("recipes/{id}") { backStackEntry ->
        val idStr = backStackEntry.arguments?.getString("id") ?: return@composable
        val id = remember(idStr) { UUID.fromString(idStr) }
        val recipesViewModel: RecipesViewModel = viewModel()

        LaunchedEffect(id) {
            recipesViewModel.loadRecipeWithId(id)
        }

        val recipe by recipesViewModel.currentRecipe.collectAsState()
        val parent by recipesViewModel.currentParent.collectAsState()
        val diff by recipesViewModel.currentDiff.collectAsState()
        val currentUsername = remember { ForkEatApi.getCurrentUsername() }

        when (val r = recipe) {
            null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = fr.uge.android.forkeat.designsystem.theme.Primary500)
            }
            else -> RecipeDetailScreen(
                recipe = r,
                parent = parent,
                diff = diff,
                isOwner = currentUsername != null && currentUsername == r.username,
                isAuthenticated = isLoggedIn,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("recipe-form?recipeId=${r.id}") },
                onDelete = {
                    recipesViewModel.deleteRecipe(r.id) {
                        navController.popBackStack()
                    }
                },
                onCreateVariant = { navController.navigate("recipe-form?parentId=${r.id}") }
            )
        }
    }

    composable("my-recipes") {
        val myRecipesViewModel: MyRecipesViewModel = viewModel()
        MyRecipesScreen(
            viewModel = myRecipesViewModel,
            isLoggedIn = isLoggedIn,
            onLogout = onLogout,
            onNavigateToCreateRecipe = { navController.navigate("recipe-form") },
            onNavigateToEdit = { recipeId ->
                navController.navigate("recipe-form?recipeId=$recipeId")
            },
            onNavigateToVariant = { parentId ->
                navController.navigate("recipe-form?parentId=$parentId")
            },
            onNavigateToDetail = { recipeId ->
                navController.navigate("recipes/$recipeId")
            },
            onBack = { navController.popBackStack() }
        )
    }
}
