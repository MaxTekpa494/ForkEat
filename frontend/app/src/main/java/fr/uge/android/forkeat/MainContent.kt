package fr.uge.android.forkeat

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.uge.android.forkeat.dashboard.DashboardScreen
import fr.uge.android.forkeat.home.ForkEatScaffold
import fr.uge.android.forkeat.home.ForgotPasswordCodeScreen
import fr.uge.android.forkeat.home.ForgotPasswordScreen
import fr.uge.android.forkeat.home.HomeScreen
import fr.uge.android.forkeat.home.LoginScreen
import fr.uge.android.forkeat.home.RegisterScreen
import fr.uge.android.forkeat.profile.ProfileScreen
import fr.uge.android.forkeat.profile.ProfileGuestScreen
import fr.uge.android.forkeat.recipes.RecipeDetailScreen
import fr.uge.android.forkeat.recipes.RecipesListScreen
import fr.uge.android.forkeat.recipes.RecipesViewModel
import fr.uge.android.forkeat.network.TokenManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.padding

@Composable
fun MainContent(recipesViewModel: RecipesViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val hideBarsRoutes = listOf("login", "register", "forgot-password", "forgot-password-code")
    val shouldShowBars = currentRoute !in hideBarsRoutes && currentRoute != null

    ForkEatScaffold(
        navController = navController,
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
                    onNavigateToRecipes = { diet -> navController.navigate("recipes?diet=$diet") },
                    showNavBar = shouldShowBars,
                )
            }
            composable("login") {
                LoginScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRegister = { navController.navigate("register") },
                    onLoginSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onForgotPassword = { navController.navigate("forgot-password") }
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
                DashboardScreen(
                    onNavigateToProfile = { navController.navigate("profile") }
                )
            }
            composable("profile") {
                val context = LocalContext.current
                val tokenManager = remember(context) { TokenManager(context) }
                if (tokenManager.isLoggedIn()) {
                    ProfileScreen()
                } else {
                    ProfileGuestScreen(
                      onNavigateToLogin = { navController.navigate("login") },
                      onNavigateToRegister = { navController.navigate("register") }
                    )
                }
            }
            composable(
                "recipes?diet={diet}",
                arguments = listOf(navArgument("diet") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val dietFilter = backStackEntry.arguments?.getString("diet")
                val recipes by recipesViewModel.recipes.collectAsState()
                val totalCount by recipesViewModel.totalCount.collectAsState()
                val errorMessage by recipesViewModel.errorMessage.collectAsState()
                val searchQuery by recipesViewModel.searchQuery.collectAsState()
                val selectedAllergens by recipesViewModel.selectedAllergens.collectAsState()
                val availableAllergens by recipesViewModel.availableAllergens.collectAsState()
                val isLoading by recipesViewModel.isLoading.collectAsState()

                if (!dietFilter.isNullOrBlank()) {
                    recipesViewModel.setDietFilter(dietFilter)
                }

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
