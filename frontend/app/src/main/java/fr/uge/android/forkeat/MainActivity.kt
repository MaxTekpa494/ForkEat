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
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ForkEatTheme {
                val navController = rememberNavController()
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
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                        )
                    }
                    composable("recipes") {
                        val viewModel: RecipesViewModel = viewModel()
                        val recipesState = viewModel.recipes.collectAsState()
                        val totalCountState = viewModel.totalCount.collectAsState()
                        val currentPageState = viewModel.currentPage.collectAsState()
                        val errorMessageState = viewModel.errorMessage.collectAsState()
                        val pageSizeState = viewModel.pageSize.collectAsState()

                        RecipesListScreen(
                            recipes = recipesState.value,
                            totalCount = totalCountState.value,
                            currentPage = currentPageState.value,
                            pageSize = pageSizeState.value,
                            onPageChange = { page -> viewModel.loadRecipes(page) },
                            errorMessage = errorMessageState.value,
                            navController = navController,
                            onSearch = { query ->
                                // TODO: connecter à l'endpoint backend de recherche
                            }
                        )
                    }
                    composable("recipes/{id}") { backStackEntry ->
                        val viewModel: RecipesViewModel = viewModel()
                        val recipes = viewModel.recipes.collectAsState().value
                        val recipeId = backStackEntry.arguments?.getString("id")
                        val recipe = recipes.find { it.id.toString() == recipeId }
                        if (recipe != null) {
                            RecipeDetailScreen(recipe = recipe, onBack = { navController.popBackStack() })
                        } else {
                            Text("Recette introuvable", color = Color.Red)
                        }
                    }
                }
            }
        }
    }
}
