package fr.uge.android.forkeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.uge.android.forkeat.dashboard.DashboardScreen
import fr.uge.android.forkeat.designsystem.theme.ForkEatTheme
import fr.uge.android.forkeat.home.ForgotPasswordCodeScreen
import fr.uge.android.forkeat.home.ForgotPasswordScreen
import fr.uge.android.forkeat.home.HomeScreen
import fr.uge.android.forkeat.home.LoginScreen
import fr.uge.android.forkeat.home.RegisterScreen
import fr.uge.android.forkeat.profile.ProfileScreen
import fr.uge.android.forkeat.recipes.RecipeDetailScreen
import fr.uge.android.forkeat.recipes.RecipesListScreen
import fr.uge.android.forkeat.recipes.RecipesViewModel

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
                            navController = navController,
                            onNavigateToExplore = {navController.navigate("recipes")}
                        )
                    }
                    composable("login") {
                        LoginScreen(
                            onNavigateBack = { navController.navigate("home") },
                            onNavigateToRegister = {  navController.navigate("register") },
                            onLoginSuccess = {
                                navController.navigate("dashboard") { // Navigate to dashboard on successful login
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onForgotPassword = {
                                navController.navigate("forgot-password")
                            }
                        )
                    }
                    composable("forgot-password"){
                       ForgotPasswordScreen(onNavigateBack = { navController.popBackStack() },onAskingSuccess = { navController.navigate("forgot-password-code")})
                    }
                    composable("forgot-password-code"){
                        ForgotPasswordCodeScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onCodeSuccess =  { navController.navigate("login")}
                        )
                    }
                    composable("new-user-login") {
                        LoginScreen(
                            onNavigateBack = { navController.navigate("home") },
                            onNavigateToRegister = {  navController.navigate("register") },
                            onLoginSuccess = {
                                navController.navigate("dashboard") { // Navigate to dashboard on successful new user login
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            newUser = true
                        )
                    }

                    composable("register") {
                        RegisterScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToLogin = {  navController.navigate("login") },
                            onRegisterSuccess = {
                                navController.navigate("dashboard") { // Navigate to dashboard on successful registration
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                        )
                    }
                    composable("dashboard") {
                        DashboardScreen(
                            onNavigateToProfile = {
                                navController.navigate("profile")
                            }
                        )
                    }
                    composable("profile") {
                        ProfileScreen()
                    }
                    composable("recipes") {
                        val recipesState = recipesViewModel.recipes.collectAsState()
                        val totalCountState = recipesViewModel.totalCount.collectAsState()
                        val errorMessageState = recipesViewModel.errorMessage.collectAsState()
                        val searchQueryState = recipesViewModel.searchQuery.collectAsState()
                        val selectedAllergensState = recipesViewModel.selectedAllergens.collectAsState()
                        val availableAllergensState = recipesViewModel.availableAllergens.collectAsState()
                        val isLoadingState = recipesViewModel.isLoading.collectAsState()

                        RecipesListScreen(
                            recipes = recipesState.value,
                            totalCount = totalCountState.value,
                            onLoadMore = { recipesViewModel.loadMoreRecipes() },
                            errorMessage = errorMessageState.value,
                            isLoading = isLoadingState.value,
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
