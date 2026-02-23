package fr.uge.android.forkeat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import fr.uge.android.forkeat.home.ForkEatScaffold
import fr.uge.android.forkeat.home.HomeScreen
import fr.uge.android.forkeat.home.LoginScreen
import fr.uge.android.forkeat.home.RegisterScreen
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.profile.ProfileScreen
import fr.uge.android.forkeat.recipes.RecipeDetailScreen
import fr.uge.android.forkeat.recipes.RecipesListScreen
import fr.uge.android.forkeat.recipes.RecipesViewModel
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.currentBackStackEntryAsState
import fr.uge.android.forkeat.profile.ProfileGuestScreen
import fr.uge.android.forkeat.wallet.WalletScreen
import kotlin.collections.contains

class MainActivity : ComponentActivity() {
    private var pendingDeepLink: String? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme == "forkeat" && data.host == "wallet") {
            pendingDeepLink = "wallet"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ForkEatApi.init(this)
        handleDeepLink(intent)
        enableEdgeToEdge()
        setContent {
            ForkEatTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val hideBarsRoutes = listOf("login", "register", "forgot-password", "forgot-password-code", "recipes/{id}")
                val shouldShowBars = currentRoute !in hideBarsRoutes && currentRoute != null
                var isLoggedIn by remember { mutableStateOf(ForkEatApi.isLoggedIn()) }
                // ViewModel partage pour toute la navigation
                val recipesViewModel: RecipesViewModel = viewModel()

                val logout: () -> Unit = {
                    ForkEatApi.logout()
                    isLoggedIn = false
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
                // Handle deep link navigation
                LaunchedEffect(Unit) {
                    pendingDeepLink?.let { dest ->
                        pendingDeepLink = null
                        navController.navigate(dest) {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                }


                ForkEatScaffold(
                    navController = navController,
                    onLogout = logout,
                    isLoggedIn = isLoggedIn,
                    showBars = shouldShowBars
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(
                                onNavigateToExplore = { navController.navigate("recipes") },
                            )
                        }
                        composable("login") {
                            LoginScreen(
                                onNavigateBack = { navController.navigate("home") },
                                onNavigateToRegister = { navController.navigate("register") },
                                onLoginSuccess = {
                                    isLoggedIn = true
                                    navController.navigate("recipes") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onForgotPassword = {
                                    navController.navigate("forgot-password")
                                }
                            )
                        }
                        composable("forgot-password") {
                            ForgotPasswordScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onAskingSuccess = { navController.navigate("forgot-password-code") }
                            )
                        }
                        composable("forgot-password-code") {
                            ForgotPasswordCodeScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onCodeSuccess = { navController.navigate("login") }
                            )
                        }
                        composable("new-user-login") {
                            LoginScreen(
                                onNavigateBack = { navController.navigate("home") },
                                onNavigateToRegister = { navController.navigate("register") },
                                onLoginSuccess = {
                                    isLoggedIn = true
                                    navController.navigate("recipes") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                newUser = true
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToLogin = { navController.navigate("login") },
                                onRegisterSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("dashboard") {
                            ForkEatScaffold(
                                navController = navController,
                                isLoggedIn = isLoggedIn,
                                onLogout = logout
                            ) {
                                DashboardScreen(
                                    onNavigateToProfile = { navController.navigate("profile") },
                                    onNavigateToWallet = { navController.navigate("wallet") }
                                )
                            }
                        }
                        composable("profile") {
                            if (isLoggedIn) {
                                ProfileScreen()
                            } else {
                                ProfileGuestScreen(
                                    onNavigateToLogin = { navController.navigate("login") },
                                    onNavigateToRegister = { navController.navigate("register") }
                                )
                            }
                        }
                        composable("wallet") {
                            ForkEatScaffold(
                                navController = navController,
                                isLoggedIn = isLoggedIn,
                                onLogout = logout
                            ) {
                                WalletScreen(
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable(
                            "recipes",
                        ) { val recipes by recipesViewModel.recipes.collectAsState()
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
}
