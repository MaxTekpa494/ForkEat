package fr.uge.android.forkeat.admin

import androidx.compose.runtime.Composable
import fr.uge.android.forkeat.moderator.ModeratorReportsScreen

@Composable
fun AdminReportsScreen(
    currentRoute: String,
    onNavigateToDashboard: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onNavigateToWallets: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToRecipe: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit
) {
    AdminScaffold(
        currentRoute = currentRoute,
        onNavigateToDashboard = onNavigateToDashboard,
        onNavigateToUsers = onNavigateToUsers,
        onNavigateToRecipes = onNavigateToRecipes,
        onNavigateToReports = {},
        onNavigateToWallets = onNavigateToWallets,
        onNavigateToCreate = onNavigateToCreate,
        onLogout = onLogout
    ) { _ ->
        ModeratorReportsScreen(
            currentRoute = "admin-reports",
            onNavigateToRecipes = onNavigateToRecipes,
            onExit = onNavigateToDashboard,
            onNavigateToRecipe = onNavigateToRecipe,
            onNavigateToUserProfile = onNavigateToUserProfile,
            adminTheme = true
        )
    }
}
