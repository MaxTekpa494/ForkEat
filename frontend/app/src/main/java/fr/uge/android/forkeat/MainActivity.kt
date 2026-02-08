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
import fr.uge.android.forkeat.home.HomeScreen
import fr.uge.android.forkeat.home.LoginScreen
import fr.uge.android.forkeat.home.RegisterScreen
import fr.uge.android.forkeat.profile.ProfileScreen

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
                            onNavigateToRegister = { navController.navigate("register") }
                        )
                    }
                    composable("login") {
                        LoginScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToRegister = {  navController.navigate("register") },
                            onLoginSuccess = {
                                navController.navigate("dashboard") { // Navigate to dashboard on successful login
                                    popUpTo("home") { inclusive = true }
                                }
                            },
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
                }
            }
        }
    }
}
