package fr.uge.android.forkeat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.uge.android.forkeat.account.AccountScreen
import fr.uge.android.forkeat.admin.AdminCreateUserScreen
import fr.uge.android.forkeat.admin.AdminDashboardScreen
import fr.uge.android.forkeat.admin.AdminRecipesScreen
import fr.uge.android.forkeat.admin.AdminReportsScreen
import fr.uge.android.forkeat.admin.AdminUsersScreen
import fr.uge.android.forkeat.admin.AdminWalletsScreen
import fr.uge.android.forkeat.designsystem.theme.ForkEatTheme
import fr.uge.android.forkeat.home.ForgotPasswordCodeScreen
import fr.uge.android.forkeat.home.ForgotPasswordScreen
import fr.uge.android.forkeat.home.ForkEatScaffold
import fr.uge.android.forkeat.home.HomeScreen
import fr.uge.android.forkeat.home.LoginScreen
import fr.uge.android.forkeat.home.RegisterScreen
import fr.uge.android.forkeat.home.WelcomeScreen
import fr.uge.android.forkeat.moderator.ModeratorRecipesScreen
import fr.uge.android.forkeat.moderator.ModeratorReportsScreen
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.profile.ProfileScreen
import fr.uge.android.forkeat.profile.UserProfileScreen
import fr.uge.android.forkeat.recipes.MyRecipesScreen
import fr.uge.android.forkeat.recipes.MyRecipesViewModel
import fr.uge.android.forkeat.recipes.RecipeDetailScreen
import fr.uge.android.forkeat.recipes.RecipeFormScreen
import fr.uge.android.forkeat.recipes.RecipeFormViewModel
import fr.uge.android.forkeat.recipes.RecipesListScreen
import fr.uge.android.forkeat.recipes.RecipesViewModel
import fr.uge.android.forkeat.recipes.SmartSearchScreen
import fr.uge.android.forkeat.recipes.SmartSearchViewModel
import fr.uge.android.forkeat.wallet.WalletScreen
import java.util.UUID

@Composable
fun AuthenticatedScreen(
  isLoggedIn: Boolean,
  onNavigateToWelcome: () -> Unit,
  content: @Composable () -> Unit
) {
  if (isLoggedIn) {
    content()
  } else {
    LaunchedEffect(Unit) {
      onNavigateToWelcome()
    }
  }
}

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
        val hideBarsRoutes =
          listOf("login", "register", "welcome", "forgot-password", "forgot-password-code")
        val shouldShowBars = currentRoute != null
                && currentRoute !in hideBarsRoutes
                && !currentRoute.startsWith("admin-")
                && !currentRoute.startsWith("moderator-")
        var isLoggedIn by remember { mutableStateOf(ForkEatApi.isLoggedIn()) }
        var isAdmin by remember { mutableStateOf(ForkEatApi.isAdmin()) }
        var isModerator by remember { mutableStateOf(ForkEatApi.isModerator()) }
        var startDestination by remember { mutableStateOf(if (isAdmin) "admin-dashboard" else "home") }
        val recipesViewModel: RecipesViewModel = viewModel()
        val smartSearchViewModel: SmartSearchViewModel = viewModel()

        var showWelcomeOnLaunch by remember { mutableStateOf(!isLoggedIn) }

        // ── Actions recette partagées ─────────────────────────────────────
        // Chaque action met à jour les deux ViewModels : recipesViewModel
        // (liste principale) ET smartSearchViewModel (résultats de recherche).
        // Ainsi un like depuis la fiche recette ou la liste principale se
        // reflète immédiatement dans les résultats du smart search, et vice-versa.
        val likeRecipe: (UUID) -> Unit = { id ->
          recipesViewModel.likeRecipe(id)
          smartSearchViewModel.updateLikeState(id, liked = true)
        }
        val unlikeRecipe: (UUID) -> Unit = { id ->
          recipesViewModel.unlikeRecipe(id)
          smartSearchViewModel.updateLikeState(id, liked = false)
        }
        val followRecipe: (UUID) -> Unit = { id ->
          recipesViewModel.followRecipe(id)
          smartSearchViewModel.updateFollowState(id, followed = true)
        }
        val unfollowRecipe: (UUID) -> Unit = { id ->
          recipesViewModel.unfollowRecipe(id)
          smartSearchViewModel.updateFollowState(id, followed = false)
        }
        val superLikeRecipe: (UUID) -> Unit = { id ->
          recipesViewModel.superLikeRecipe(id)
          smartSearchViewModel.updateSuperLikeState(id)
        }

        val logout: () -> Unit = {
          ForkEatApi.logout()
          isLoggedIn = false
          isAdmin = false
          isModerator = false
          startDestination = "home"
          recipesViewModel.loadRecipes(0)
          navController.navigate("home") {
            popUpTo(0) { inclusive = true }
          }
        }

        val redirectToWelcome: () -> Unit = {
          val route = currentRoute ?: "home"
          navController.navigate("welcome") {
            popUpTo(route) { inclusive = true }
          }
        }

        val runAuth: (() -> Unit) -> Unit = { action ->
          if (isLoggedIn) {
            action()
          } else {
            navController.navigate("welcome")
          }
        }

        LaunchedEffect(Unit) {
          pendingDeepLink?.let { dest ->
            pendingDeepLink = null
            navController.navigate(dest) {
              popUpTo(startDestination) { inclusive = false }
            }
          }
        }
        if (showWelcomeOnLaunch && currentRoute == "home") {
          WelcomeScreen(
            onNavigateToLogin = {
              showWelcomeOnLaunch = false
              navController.navigate("login")
            },
            onNavigateToRegister = {
              showWelcomeOnLaunch = false
              navController.navigate("register")
            },
            onDismiss = { showWelcomeOnLaunch = false }
          )
        } else ForkEatScaffold(
          navController = navController,
          onLogout = logout,
          isLoggedIn = isLoggedIn,
          showBars = shouldShowBars,
          isModerator = isModerator
        ) { innerPadding ->
          NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
          ) {
            composable("home") {
              HomeScreen(
                onNavigateToExplore = { navController.navigate("recipes") }
              )
            }
            composable("welcome") {
              WelcomeScreen(
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToRegister = { navController.navigate("register") },
                onDismiss = {
                  if (!navController.popBackStack()) {
                    navController.navigate("home")
                  }
                }
              )
            }
            composable("login") {
              LoginScreen(
                onNavigateBack = { navController.navigate("home") },
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = {
                  isLoggedIn = true
                  isAdmin = ForkEatApi.isAdmin()
                  isModerator = ForkEatApi.isModerator()
                  startDestination = if (isAdmin) "admin-dashboard" else "home"
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
                  isModerator = ForkEatApi.isModerator()
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
            composable("profile") {
              AuthenticatedScreen(isLoggedIn, redirectToWelcome) {
                ProfileScreen(
                  onNavigateToMyRecipes = { navController.navigate("my-recipes") },
                  onNavigateToCreateRecipe = { navController.navigate("recipe-form") },
                  onNavigateToWallet = { navController.navigate("wallet") },
                  onNavigateToAccount = { navController.navigate("account") }
                )
              }

            }
            composable("account") {
              AuthenticatedScreen(isLoggedIn, redirectToWelcome) {
                AccountScreen(onNavigateBack = { navController.popBackStack() })
              }
            }
            composable("wallet") {
              AuthenticatedScreen(isLoggedIn, redirectToWelcome) {
                WalletScreen(onNavigateBack = { navController.popBackStack() })
              }
            }


            composable("recipes") {
              val recipes by recipesViewModel.recipes.collectAsState()
              val totalCount by recipesViewModel.totalCount.collectAsState()
              val errorMessage by recipesViewModel.errorMessage.collectAsState()
              val searchQuery by recipesViewModel.searchQuery.collectAsState()
              val selectedAllergens by recipesViewModel.selectedAllergens.collectAsState()
              val availableAllergensDto by recipesViewModel.availableAllergens.collectAsState()
              val isLoading by recipesViewModel.isLoading.collectAsState()
              val insufficientFunds by recipesViewModel.insufficientFunds.collectAsState()
              val emailNotVerified by recipesViewModel.emailNotVerified.collectAsState()

              RecipesListScreen(
                recipes = recipes,
                totalCount = totalCount,
                onLoadMore = { recipesViewModel.loadMoreRecipes() },
                errorMessage = errorMessage,
                isLoading = isLoading,
                isLoggedIn = isLoggedIn,
                onNavigateToCreateRecipe = { runAuth { navController.navigate("recipe-form") } },
                searchQuery = searchQuery,
                onSearchQueryChange = { recipesViewModel.onSearchQueryChange(it) },
                onSearchSubmit = { recipesViewModel.onSearchSubmit() },
                availableAllergens = availableAllergensDto.map { it.name },
                selectedAllergens = selectedAllergens,
                onAllergenToggle = { recipesViewModel.toggleAllergen(it) },
                onClearFilters = { recipesViewModel.clearFilters() },
                onRecipeClick = { id -> navController.navigate("recipes/$id") },
                onNavigateToUserProfile = { runAuth { navController.navigate("user/$it") } },
                onNavigateToMyProfile = { runAuth { navController.navigate("profile") } },
                onLikeRecipe = { id -> runAuth { likeRecipe(id) } },
                onUnlikeRecipe = { id -> runAuth { unlikeRecipe(id) } },
                onSuperLikeRecipe = { id -> runAuth { superLikeRecipe(id) } },
                onFollowRecipe = { id -> runAuth { followRecipe(id) } },
                onUnfollowRecipe = { id -> runAuth { unfollowRecipe(id) } },
                onNavigateToLogin = { navController.navigate("welcome") },
                insufficientFunds = insufficientFunds,
                onDismissInsufficientFunds = { recipesViewModel.dismissInsufficientFunds() },
                emailNotVerified = emailNotVerified,
                onDismissEmailNotVerified = { recipesViewModel.dismissEmailNotVerified() },
                onNavigateToWallet = { navController.navigate("wallet") },
                onNavigateToAccount = { navController.navigate("account") },
                onNavigateToSmartSearch = { runAuth { navController.navigate("smart-search") } }
              )
            }
            composable(
              route = "recipe-form?recipeId={recipeId}&parentId={parentId}",
              arguments = listOf(
                navArgument("recipeId") {
                  type = NavType.StringType; nullable = true; defaultValue = null
                },
                navArgument("parentId") {
                  type = NavType.StringType; nullable = true; defaultValue = null
                }
              )
            ) {
              AuthenticatedScreen(isLoggedIn, redirectToWelcome) {
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
            }
            composable("recipes/{id}") { backStackEntry ->
              val idStr = backStackEntry.arguments?.getString("id") ?: return@composable
              val id = remember(idStr) { UUID.fromString(idStr) }

              LaunchedEffect(id) {
                recipesViewModel.loadRecipeWithId(id)
              }

              val recipe by recipesViewModel.currentRecipe.collectAsState()
              val diff by recipesViewModel.currentDiff.collectAsState()
              val insufficientFunds by recipesViewModel.insufficientFunds.collectAsState()
              val emailNotVerified by recipesViewModel.emailNotVerified.collectAsState()
              val reportSuccess by recipesViewModel.reportSuccess.collectAsState()
              val reportAlreadyReported by recipesViewModel.reportAlreadyReported.collectAsState()
              val currentUsername = remember { ForkEatApi.getCurrentUsername() }

              when (val r = recipe) {
                null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(color = fr.uge.android.forkeat.designsystem.theme.Primary500)
                }

                else -> RecipeDetailScreen(
                  recipe = r,
                  diff = diff,
                  isOwner = currentUsername != null && currentUsername == r.username,
                  isAuthenticated = isLoggedIn,
                  onBack = { navController.popBackStack() },
                  onEdit = { runAuth { navController.navigate("recipe-form?recipeId=${r.id}") } },
                  onDelete = { runAuth { recipesViewModel.deleteRecipe(r.id) { navController.popBackStack() } } },
                  onCreateVariant = { runAuth { navController.navigate("recipe-form?parentId=${r.id}") } },
                  onNavigateToUserProfile = { runAuth { navController.navigate("user/$it") } },
                  onNavigateToMyProfile = { runAuth { navController.navigate("profile") } },
                  onLike = { id -> runAuth { recipesViewModel.likeRecipe(id) } },
                  onUnlike = { id -> runAuth { recipesViewModel.unlikeRecipe(id) } },
                  onSuperLike = { id -> runAuth { recipesViewModel.superLikeRecipe(id) } },
                  onFollow = { id -> runAuth { recipesViewModel.followRecipe(id) } },
                  onUnfollow = { id -> runAuth { recipesViewModel.unfollowRecipe(id) } },
                  insufficientFunds = insufficientFunds,
                  onDismissInsufficientFunds = { recipesViewModel.dismissInsufficientFunds() },
                  emailNotVerified = emailNotVerified,
                  onDismissEmailNotVerified = { recipesViewModel.dismissEmailNotVerified() },
                  onNavigateToAccount = { navController.navigate("account") },
                  onNavigateToWallet = { navController.navigate("wallet") },
                  onNavigateToLogin = { navController.navigate("welcome") },
                  onReport = { id, type, justification ->
                    recipesViewModel.reportRecipe(id, type, justification)
                  },
                  reportSuccess = reportSuccess,
                  onDismissReportSuccess = { recipesViewModel.dismissReportSuccess() },
                  reportAlreadyReported = reportAlreadyReported,
                  onDismissReportAlreadyReported = { recipesViewModel.dismissReportAlreadyReported() }
                )
              }
            }
            composable("user/{username}") { backStackEntry ->
              val username = backStackEntry.arguments?.getString("username") ?: return@composable
              AuthenticatedScreen(isLoggedIn, { navController.navigate("welcome") }) {
                UserProfileScreen(
                  username = username,
                  onNavigateBack = { navController.popBackStack() },
                  onNavigateToRecipe = { id -> navController.navigate("recipes/$id") },
                  onNavigateToLogin = { navController.navigate("welcome") },
                  onNavigateToWallet = { navController.navigate("wallet") },
                  onNavigateToAccount = { navController.navigate("account") }
                )
              }
            }
            composable("my-recipes") {
              AuthenticatedScreen(isLoggedIn, { navController.navigate("welcome") }) {
                val myRecipesViewModel: MyRecipesViewModel = viewModel()
                MyRecipesScreen(
                  viewModel = myRecipesViewModel,
                  isLoggedIn = isLoggedIn,
                  onLogout = logout,
                  onNavigateToCreateRecipe = { navController.navigate("recipe-form") },
                  onNavigateToEdit = { navController.navigate("recipe-form?recipeId=$it") },
                  onNavigateToVariant = { navController.navigate("recipe-form?parentId=$it") },
                  onNavigateToDetail = { navController.navigate("recipes/$it") },
                  onBack = { navController.popBackStack() }
                )
              }
            }
            composable("smart-search") {
              AuthenticatedScreen(isLoggedIn, redirectToWelcome) {
                val query by smartSearchViewModel.query.collectAsState()
                val results by smartSearchViewModel.results.collectAsState()
                val isLoading by smartSearchViewModel.isLoading.collectAsState()
                val hasSearched by smartSearchViewModel.hasSearched.collectAsState()
                val error by smartSearchViewModel.error.collectAsState()
                val isModerationError by smartSearchViewModel.isModerationError.collectAsState()
                val balance by smartSearchViewModel.balance.collectAsState()
                val smartSearchCost by smartSearchViewModel.smartSearchCost.collectAsState()

                SmartSearchScreen(
                  query = query,
                  results = results,
                  isLoading = isLoading,
                  hasSearched = hasSearched,
                  error = error,
                  isModerationError = isModerationError,
                  balance = balance,
                  smartSearchCost = smartSearchCost,
                  onQueryChange = { smartSearchViewModel.onQueryChange(it) },
                  onSearch = { smartSearchViewModel.search() },
                  onNewSearch = { smartSearchViewModel.newSearch() },
                  onBack = { navController.popBackStack() },
                  onRecipeClick = { id -> navController.navigate("recipes/$id") },
                  onNavigateToWallet = { navController.navigate("wallet") },
                  isLoggedIn = isLoggedIn,
                  onNavigateToUserProfile = { runAuth { navController.navigate("user/$it") } },
                  onNavigateToMyProfile = { runAuth { navController.navigate("profile") } },
                  onNavigateToLogin = { navController.navigate("welcome") },
                  onLike = { id -> runAuth { likeRecipe(id) } },
                  onUnlike = { id -> runAuth { unlikeRecipe(id) } },
                  onSuperLike = { id -> runAuth { superLikeRecipe(id) } },
                  onFollow = { id -> runAuth { followRecipe(id) } },
                  onUnfollow = { id -> runAuth { unfollowRecipe(id) } },
                )
              }
            }
            composable("admin-dashboard") {
              AdminDashboardScreen(
                currentRoute = "admin-dashboard",
                onNavigateToMembers = { navController.navigate("admin-users?tab=0") },
                onNavigateToModerators = { navController.navigate("admin-users?tab=1") },
                onNavigateToAdmins = { navController.navigate("admin-users?tab=2") },
                onNavigateToRecipes = { navController.navigate("admin-recipes") },
                onNavigateToReports = { navController.navigate("admin-reports") },
                onNavigateToWallets = { navController.navigate("admin-wallets") },
                onNavigateToCreate = { navController.navigate("admin-create") },
                onLogout = logout
              )
            }
            composable("admin-reports") {
              AdminReportsScreen(
                currentRoute = "admin-reports",
                onNavigateToDashboard = { navController.navigate("admin-dashboard") },
                onNavigateToUsers = { navController.navigate("admin-users") },
                onNavigateToRecipes = { navController.navigate("admin-recipes") },
                onNavigateToWallets = { navController.navigate("admin-wallets") },
                onNavigateToCreate = { navController.navigate("admin-create") },
                onLogout = logout,
                onNavigateToRecipe = { id -> navController.navigate("recipes/$id") },
                onNavigateToUserProfile = { username -> navController.navigate("user/$username") }
              )
            }
            composable(
              route = "admin-users?tab={tab}",
              arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })
            ) { backStackEntry ->
              val tab = backStackEntry.arguments?.getInt("tab") ?: 0
              AdminUsersScreen(
                currentRoute = "admin-users",
                initialTab = tab,
                onNavigateToDashboard = { navController.navigate("admin-dashboard") },
                onNavigateToRecipes = { navController.navigate("admin-recipes") },
                onNavigateToWallets = { navController.navigate("admin-wallets") },
                onNavigateToCreate = { navController.navigate("admin-create") },
                onNavigateToReports = { navController.navigate("admin-reports") },
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
                onNavigateToReports = { navController.navigate("admin-reports") },
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
                onNavigateToReports = { navController.navigate("admin-reports") },
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
                onNavigateToReports = { navController.navigate("admin-reports") },
                onLogout = logout
              )
            }
            // Moderator routes
            composable("moderator-recipes") {
              ModeratorRecipesScreen(
                currentRoute = "moderator-recipes",
                onNavigateToReports = { navController.navigate("moderator-reports") },
                onNavigateToRecipe = { id -> navController.navigate("recipes/$id") },
                onExit = { navController.navigate("recipes") }
              )
            }
            composable("moderator-reports") {
              ModeratorReportsScreen(
                currentRoute = "moderator-reports",
                onNavigateToRecipes = { navController.navigate("moderator-recipes") },
                onExit = { navController.navigate("recipes") },
                onNavigateToRecipe = { id -> navController.navigate("recipes/$id") },
                onNavigateToUserProfile = { username -> navController.navigate("user/$username") }
              )
            }
          }
        }
      }
    }
  }
}
