package fr.uge.android.forkeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.uge.android.forkeat.designsystem.theme.ForkEatTheme
import fr.uge.android.forkeat.dashboard.DashboardScreen
import fr.uge.android.forkeat.home.ForgotPasswordScreen
import fr.uge.android.forkeat.home.ForgotPasswordCodeScreen
import fr.uge.android.forkeat.home.HomeScreen
import fr.uge.android.forkeat.home.LoginScreen
import fr.uge.android.forkeat.home.RegisterScreen
import fr.uge.android.forkeat.profile.ProfileScreen
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
                            onNavigateToExplore = {navController.navigate("recipes")}
                            //onNavigateToRegister = { navController.navigate("register") }
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
                        val currentPageState = recipesViewModel.currentPage.collectAsState()
                        val errorMessageState = recipesViewModel.errorMessage.collectAsState()
                        val pageSizeState = recipesViewModel.pageSize.collectAsState()

                        RecipesListScreen(
                            recipes = recipesState.value,
                            totalCount = totalCountState.value,
                            currentPage = currentPageState.value,
                            pageSize = pageSizeState.value,
                            onPageChange = { page -> recipesViewModel.loadRecipes(page) },
                            errorMessage = errorMessageState.value,
                            navController = navController,
                            onSearch = { query ->
                                // TODO: connecter à l'endpoint backend de recherche
                            }
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
