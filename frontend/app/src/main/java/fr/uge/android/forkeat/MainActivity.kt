package fr.uge.android.forkeat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.currentBackStackEntryAsState
import fr.uge.android.forkeat.admin.AdminCreateUserScreen
import fr.uge.android.forkeat.admin.AdminDashboardScreen
import fr.uge.android.forkeat.admin.AdminRecipesScreen
import fr.uge.android.forkeat.admin.AdminUsersScreen
import fr.uge.android.forkeat.admin.AdminWalletsScreen
import fr.uge.android.forkeat.profile.ProfileGuestScreen
import fr.uge.android.forkeat.wallet.WalletScreen
import kotlin.collections.contains
import java.util.UUID

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
                val adminRoutes = listOf("admin-dashboard", "admin-recipes", "admin-wallets", "admin-create")
                val hideBarsRoutes = listOf("login", "register", "forgot-password", "forgot-password-code", "recipes/{id}") + adminRoutes
                val shouldShowBars = currentRoute !in hideBarsRoutes
                    && currentRoute?.startsWith("admin-users") != true
                    && currentRoute != null
                var isCheckingAuth by remember { mutableStateOf(ForkEatApi.isLoggedIn()) }
                var isLoggedIn by remember { mutableStateOf(false) }
                var isAdmin by remember { mutableStateOf(false) }
                // ViewModel partage pour toute la navigation
                val recipesViewModel: RecipesViewModel = viewModel()

                val logout: () -> Unit = {
                    ForkEatApi.logout()
                    isLoggedIn = false
                    isAdmin = false
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
                // Validation du token au démarrage + deep link
                LaunchedEffect(Unit) {
                    if (ForkEatApi.isLoggedIn()) {
                        try {
                            val response = ForkEatApi.authService.me()
                            if (response.isSuccessful && response.body() != null) {
                                val role = response.body()!!.resource.role
                                isLoggedIn = true
                                isAdmin = role == "ADMIN"
                            } else {
                                ForkEatApi.logout()
                            }
                        } catch (_: Exception) {
                            ForkEatApi.logout()
                        }
                    }
                    isCheckingAuth = false
                    pendingDeepLink?.let { dest ->
                        pendingDeepLink = null
                        navController.navigate(dest) {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                }
              if (isCheckingAuth) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = fr.uge.android.forkeat.designsystem.theme.Primary500)
                }
              } else ForkEatScaffold(
                navController = navController,
                onLogout = logout,
                isLoggedIn = isLoggedIn,
                showBars = shouldShowBars
              ) { innerPadding ->
                NavHost(navController = navController, startDestination = "home", modifier = Modifier.padding(innerPadding)) {
                  composable("home") {
                    HomeScreen(
                      onNavigateToExplore = { navController.navigate("recipes") }
                    )
                  }
                  composable("login") {
                    LoginScreen(
                      onNavigateBack = { navController.navigate("home") },
                      onNavigateToRegister = { navController.navigate("register") },
                      onLoginSuccess = {
                        isLoggedIn = true
                        isAdmin = ForkEatApi.isAdmin()
                        val destination = if (isAdmin) "admin-dashboard" else "recipes"
                        navController.navigate(destination) {
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
                        isAdmin = ForkEatApi.isAdmin()
                        val destination = if (isAdmin) "admin-dashboard" else "recipes"
                        navController.navigate(destination) {
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
                    DashboardScreen(
                      onNavigateToProfile = { navController.navigate("profile") },
                      onNavigateToWallet = { navController.navigate("wallet") },
                      onNavigateToRecipes = { navController.navigate("recipes") },
                      onNavigateToCreateRecipe = { navController.navigate("recipe-form") }
                    )
                  }
                  composable("profile") {
                    if (isLoggedIn) {
                      ProfileScreen(
                        onNavigateToCreateRecipe = { navController.navigate("recipe-form") },
                        onNavigateToMyRecipes = { navController.navigate("my-recipes") })
                    } else {
                      ProfileGuestScreen(
                        onNavigateToLogin = { navController.navigate("login") },
                        onNavigateToRegister = { navController.navigate("register") }
                      )
                    }

                  }
                  composable("wallet") {
                    WalletScreen(
                      onNavigateBack = { navController.popBackStack() }
                    )
                  }
                  composable("recipes") {
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
                                recipesViewModel.loadRecipes(0)
                                navController.navigate("recipes") {
                                    popUpTo("recipe-form") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("recipes/{id}") { backStackEntry ->
                        val idStr = backStackEntry.arguments?.getString("id") ?: return@composable
                        val id = remember(idStr) { UUID.fromString(idStr) }

                        LaunchedEffect(id) {
                            recipesViewModel.loadRecipeWithId(id)
                        }

                        val recipe by recipesViewModel.currentRecipe.collectAsState()
                        val parent by recipesViewModel.currentParent.collectAsState()
                        val diff   by recipesViewModel.currentDiff.collectAsState()
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
                            onLogout = logout,
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
                    // ── Admin routes ───────────────────────────────────────────────
                    composable("admin-dashboard") {
                        AdminDashboardScreen(
                            currentRoute = "admin-dashboard",
                            onNavigateToMembers = { navController.navigate("admin-users?tab=0") },
                            onNavigateToModerators = { navController.navigate("admin-users?tab=1") },
                            onNavigateToAdmins = { navController.navigate("admin-users?tab=2") },
                            onNavigateToRecipes = { navController.navigate("admin-recipes") },
                            onNavigateToWallets = { navController.navigate("admin-wallets") },
                            onNavigateToCreate = { navController.navigate("admin-create") },
                            onLogout = logout
                        )
                    }
                    composable(
                        route = "admin-users?tab={tab}",
                        arguments = listOf(
                            navArgument("tab") { type = NavType.IntType; defaultValue = 0 }
                        )
                    ) { backStackEntry ->
                        val tab = backStackEntry.arguments?.getInt("tab") ?: 0
                        AdminUsersScreen(
                            currentRoute = "admin-users",
                            initialTab = tab,
                            onNavigateToDashboard = { navController.navigate("admin-dashboard") },
                            onNavigateToRecipes = { navController.navigate("admin-recipes") },
                            onNavigateToWallets = { navController.navigate("admin-wallets") },
                            onNavigateToCreate = { navController.navigate("admin-create") },
                            onLogout = logout
                        )
                    }
                    composable("admin-recipes") {
                        AdminRecipesScreen(
                            currentRoute = "admin-recipes",
                            onNavigateToDashboard = { navController.navigate("admin-dashboard") },
                            onNavigateToUsers = { navController.navigate("admin-users") },
                            onNavigateToWallets = { navController.navigate("admin-wallets") },
                            onNavigateToCreate = { navController.navigate("admin-create") },
                            onNavigateToRecipe = { id -> navController.navigate("recipes/$id") },
                            onLogout = logout
                        )
                    }
                    composable("admin-wallets") {
                        AdminWalletsScreen(
                            currentRoute = "admin-wallets",
                            onNavigateToDashboard = { navController.navigate("admin-dashboard") },
                            onNavigateToUsers = { navController.navigate("admin-users") },
                            onNavigateToRecipes = { navController.navigate("admin-recipes") },
                            onNavigateToCreate = { navController.navigate("admin-create") },
                            onLogout = logout
                        )
                    }
                    composable("admin-create") {
                        AdminCreateUserScreen(
                            currentRoute = "admin-create",
                            onNavigateToDashboard = { navController.navigate("admin-dashboard") },
                            onNavigateToUsers = { navController.navigate("admin-users") },
                            onNavigateToRecipes = { navController.navigate("admin-recipes") },
                            onNavigateToWallets = { navController.navigate("admin-wallets") },
                            onLogout = logout
                        )
                    }
                }
              }
            }
        }
    }
}
