package fr.uge.android.forkeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.uge.android.forkeat.designsystem.theme.ForkEatTheme
import fr.uge.android.forkeat.home.HomeScreen
import fr.uge.android.forkeat.home.LoginScreen
import fr.uge.android.forkeat.recipes.RecipesListScreen
import fr.uge.android.forkeat.recipes.RecipesViewModel
import fr.uge.android.forkeat.recipes.RecipeDetailScreen
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ForkEatTheme {
                val navController = rememberNavController()
                // ViewModel partagé pour toute la navigation
                val recipesViewModel: RecipesViewModel = viewModel()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            onNavigateToLogin = { navController.navigate("login") },
                            onNavigateToExplore = { navController.navigate("recipes") }
                        )
                    }
                    composable("login") {
                        LoginScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToRegister = { /* TODO: navigate to register */ },
                            onLoginSuccess = {
                                // TODO: navigate to dashboard
                                navController.navigate("recipes") {
                                    popUpTo("recipes") { inclusive = true }
                                }
                            },
                        )
                    }
                    composable("recipes") {
                        val recipesState = recipesViewModel.recipes.collectAsState()
                        val totalCountState = recipesViewModel.totalCount.collectAsState()
                        val currentPageState = recipesViewModel.currentPage.collectAsState()
                        val errorMessageState = recipesViewModel.errorMessage.collectAsState()
                        val pageSizeState = recipesViewModel.pageSize.collectAsState()
                        val searchQueryState = recipesViewModel.searchQuery.collectAsState()
                        val selectedAllergensState = recipesViewModel.selectedAllergens.collectAsState()
                        val availableAllergensState = recipesViewModel.availableAllergens.collectAsState()

                        RecipesListScreen(
                            recipes = recipesState.value,
                            totalCount = totalCountState.value,
                            currentPage = currentPageState.value,
                            pageSize = pageSizeState.value,
                            onPageChange = { page -> recipesViewModel.loadRecipes(page) },
                            errorMessage = errorMessageState.value,
                            navController = navController,
                            searchQuery = searchQueryState.value,
                            onSearchQueryChange = { query -> recipesViewModel.onSearchQueryChange(query) },
                            onSearchSubmit = { recipesViewModel.onSearchSubmit() },
                            availableAllergens = availableAllergensState.value,
                            selectedAllergens = selectedAllergensState.value,
                            onAllergenToggle = { allergen -> recipesViewModel.toggleAllergen(allergen) },
                            onClearFilters = { recipesViewModel.clearFilters() }
                        )
                    }
                    composable("recipes/{id}") { backStackEntry ->
                        val recipes = recipesViewModel.recipes.collectAsState().value
                        val recipeId = backStackEntry.arguments?.getString("id")
                        val recipe = recipes.find { it.id.toString() == recipeId }
                        if (recipe != null) {
                            RecipeDetailScreen(recipe = recipe, onBack = { navController.popBackStack() })
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Recette introuvable", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}
