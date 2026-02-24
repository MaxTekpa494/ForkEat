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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import fr.uge.android.forkeat.recipes.MyRecipesScreen
import fr.uge.android.forkeat.recipes.MyRecipesViewModel
import fr.uge.android.forkeat.recipes.RecipeDetailScreen
import fr.uge.android.forkeat.recipes.RecipeFormScreen
import fr.uge.android.forkeat.recipes.RecipeFormViewModel
import fr.uge.android.forkeat.recipes.RecipesListScreen
import fr.uge.android.forkeat.recipes.RecipesViewModel
import fr.uge.android.forkeat.wallet.WalletScreen

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
                var isLoggedIn by remember { mutableStateOf(ForkEatApi.isLoggedIn()) }

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
                // ViewModel partage pour toute la navigation
                val recipesViewModel: RecipesViewModel = viewModel()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            navController = navController,
                            onNavigateToExplore = { navController.navigate("recipes") }
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
                                isLoggedIn = true
                                navController.navigate("recipes") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
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
                                onNavigateToWallet = { navController.navigate("wallet") },
                                onNavigateToCreateRecipe = { navController.navigate("recipe-form") }
                            )
                        }
                    }
                    composable("profile") {
                        ForkEatScaffold(
                            navController = navController,
                            isLoggedIn = isLoggedIn,
                            onLogout = logout
                        ) {
                            ProfileScreen(
                                onNavigateToCreateRecipe = { navController.navigate("recipe-form") },
                                onNavigateToMyRecipes = { navController.navigate("my-recipes") }
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
                            isLoggedIn = isLoggedIn,
                            onLogout = logout,
                            onNavigateToCreateRecipe = { navController.navigate("recipe-form") },
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
                                recipesViewModel.loadRecipes(0)
                                navController.navigate("recipes") {
                                    popUpTo("recipe-form") { inclusive = true }
                                }
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
                    composable("my-recipes") {
                        val myRecipesViewModel: MyRecipesViewModel = viewModel()
                        MyRecipesScreen(
                            viewModel = myRecipesViewModel,
                            isLoggedIn = isLoggedIn,
                            onLogout = logout,
                            onNavigateToCreateRecipe = { navController.navigate("recipe-form") },
                            onNavigateToEdit = { recipeId ->
                                navController.navigate("recipe-form?recipeId=$recipeId")
                            },
                            onNavigateToVariant = { parentId ->
                                navController.navigate("recipe-form?parentId=$parentId")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
