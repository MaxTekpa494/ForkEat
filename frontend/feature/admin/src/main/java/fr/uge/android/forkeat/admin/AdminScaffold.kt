package fr.uge.android.forkeat.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Couleurs admin (violet) ──────────────────────────────────────────────────
val AdminPurple900 = Color(0xFF3B0764)
val AdminPurple800 = Color(0xFF4C1D95)
val AdminPurple700 = Color(0xFF5B21B6)
val AdminPurple600 = Color(0xFF6D28D9)
val AdminPurple500 = Color(0xFF7C3AED)
val AdminPurple300 = Color(0xFFA78BFA)
val AdminPurple200 = Color(0xFFDDD6FE)
val AdminPurple100 = Color(0xFFEDE9FE)

private enum class AdminTab { DASHBOARD, USERS, RECIPES, REPORTS, WALLETS, CREATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScaffold(
    currentRoute: String,
    onNavigateToDashboard: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToWallets: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onLogout: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val selectedTab = when (currentRoute) {
        "admin-dashboard" -> AdminTab.DASHBOARD
        "admin-users"     -> AdminTab.USERS
        "admin-recipes"   -> AdminTab.RECIPES
        "admin-reports"   -> AdminTab.REPORTS
        "admin-wallets"   -> AdminTab.WALLETS
        "admin-create"    -> AdminTab.CREATE
        else              -> AdminTab.DASHBOARD
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(AdminPurple700),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = AdminPurple200,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                                    append("Fork")
                                }
                                withStyle(SpanStyle(color = AdminPurple300, fontWeight = FontWeight.Bold)) {
                                    append("Eat")
                                }
                            },
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Admin",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AdminPurple300,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .background(AdminPurple700, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Déconnexion",
                            tint = AdminPurple300
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AdminPurple900
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = AdminPurple900,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == AdminTab.DASHBOARD,
                    onClick = { if (selectedTab != AdminTab.DASHBOARD) onNavigateToDashboard() },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Tableau de bord") },
                    label = { Text("Dashboard", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AdminPurple300,
                        selectedTextColor = AdminPurple300,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = AdminPurple700
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == AdminTab.USERS,
                    onClick = { if (selectedTab != AdminTab.USERS) onNavigateToUsers() },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Utilisateurs") },
                    label = { Text("Utilisateurs", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AdminPurple300,
                        selectedTextColor = AdminPurple300,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = AdminPurple700
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == AdminTab.RECIPES,
                    onClick = { if (selectedTab != AdminTab.RECIPES) onNavigateToRecipes() },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Recettes") },
                    label = { Text("Recettes", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AdminPurple300,
                        selectedTextColor = AdminPurple300,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = AdminPurple700
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == AdminTab.REPORTS,
                    onClick = { if (selectedTab != AdminTab.REPORTS) onNavigateToReports() },
                    icon = { Icon(Icons.Default.Report, contentDescription = "Signalements") },
                    label = { Text("Signalements", fontSize = 8.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AdminPurple300,
                        selectedTextColor = AdminPurple300,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = AdminPurple700
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == AdminTab.WALLETS,
                    onClick = { if (selectedTab != AdminTab.WALLETS) onNavigateToWallets() },
                    icon = { Icon(Icons.Default.Wallet, contentDescription = "Wallets") },
                    label = { Text("Wallets", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AdminPurple300,
                        selectedTextColor = AdminPurple300,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = AdminPurple700
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == AdminTab.CREATE,
                    onClick = { if (selectedTab != AdminTab.CREATE) onNavigateToCreate() },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Créer") },
                    label = { Text("Créer", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AdminPurple300,
                        selectedTextColor = AdminPurple300,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = AdminPurple700
                    )
                )
            }
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}
