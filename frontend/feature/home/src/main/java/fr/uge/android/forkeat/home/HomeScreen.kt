package fr.uge.android.forkeat.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import fr.uge.android.forkeat.designsystem.theme.Gray100
import fr.uge.android.forkeat.designsystem.theme.Gray500
import fr.uge.android.forkeat.designsystem.theme.Orange50
import fr.uge.android.forkeat.designsystem.theme.Orange500
import fr.uge.android.forkeat.designsystem.theme.Primary100
import fr.uge.android.forkeat.designsystem.theme.Primary300
import fr.uge.android.forkeat.designsystem.theme.Primary500
import fr.uge.android.forkeat.designsystem.theme.Secondary100
import fr.uge.android.forkeat.designsystem.theme.Secondary200
import fr.uge.android.forkeat.designsystem.theme.Secondary50
import fr.uge.android.forkeat.designsystem.theme.Secondary600
import fr.uge.android.forkeat.designsystem.theme.Secondary700
import fr.uge.android.forkeat.designsystem.theme.Secondary800
import fr.uge.android.forkeat.designsystem.theme.Secondary900
import fr.uge.android.forkeat.designsystem.theme.SurfaceCream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToExplore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        HeroSection(
            onExplorerClick = onNavigateToExplore,
        )
        FeaturesSection()
        Footer()
    }
}

// Top Bar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForkEatTopBarTitle() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Secondary50),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = Secondary800,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Secondary900, fontWeight = FontWeight.Bold)) {
                    append("Fork")
                }
                withStyle(SpanStyle(color = Secondary700, fontWeight = FontWeight.Bold)) {
                    append("Eat")
                }
            },
            fontSize = 22.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForkEatTopBar(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { ForkEatTopBarTitle() },
        actions = {
            IconButton(onClick = { menuExpanded = !menuExpanded }) {
                Icon(
                    imageVector = if (menuExpanded) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Secondary800,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Gray100),
            ) {
                Text(
                    text = "MENU",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gray500,
                    letterSpacing = 1.sp,
                )
                DropdownMenuItem(
                    text = { Text("Connexion", fontWeight = FontWeight.SemiBold, color = Secondary900) },
                    onClick = {
                        menuExpanded = false
                        onNavigateToLogin()
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = Secondary700)
                    },
                )
                DropdownMenuItem(
                    text = { Text("S'inscrire", fontWeight = FontWeight.SemiBold, color = Primary500) },
                    onClick = {
                        menuExpanded = false
                        onNavigateToRegister()
                              },
                    leadingIcon = {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Primary500)
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Gray100,
                )
                DropdownMenuItem(
                    text = { Text("Nous contacter", color = Secondary700) },
                    onClick = { menuExpanded = false },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Secondary700)
                    },
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White.copy(alpha = 0.97f),
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForkEatLoggedInTopBar(
    onNavigateToProfile: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onLogout: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { ForkEatTopBarTitle() },
        actions = {
            IconButton(onClick = { menuExpanded = !menuExpanded }) {
                Icon(
                    imageVector = if (menuExpanded) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Secondary800,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Gray100),
            ) {
                Text(
                    text = "MENU",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gray500,
                    letterSpacing = 1.sp,
                )
                DropdownMenuItem(
                    text = { Text("Mon Profil", fontWeight = FontWeight.SemiBold, color = Secondary900) },
                    onClick = { menuExpanded = false; onNavigateToProfile() },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Secondary700) },
                )
                DropdownMenuItem(
                    text = { Text("Mon compte", fontWeight = FontWeight.SemiBold, color = Secondary900) },
                    onClick = { menuExpanded = false; onNavigateToAccount() },
                    leadingIcon = { Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = Secondary700) },
                )
                DropdownMenuItem(
                    text = { Text("Mon Wallet", fontWeight = FontWeight.SemiBold, color = Secondary900) },
                    onClick = { menuExpanded = false; onNavigateToWallet() },
                    leadingIcon = { Icon(Icons.Default.Wallet, contentDescription = null, tint = Secondary700) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Gray100,
                )
                DropdownMenuItem(
                    text = { Text("Deconnexion", fontWeight = FontWeight.SemiBold, color = Primary500) },
                    onClick = { menuExpanded = false; onLogout() },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Primary500) },
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White.copy(alpha = 0.97f),
        ),
    )
}

// Hero Section

@Composable
private fun HeroSection(
    onExplorerClick: () -> Unit,
) {
    val heroGradient = Brush.verticalGradient(
        colors = listOf(Secondary800, Secondary900)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(heroGradient)
            .padding(horizontal = 24.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Badge
            Text(
                text = "LA PLATEFORME GOURMANDE #1",
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Secondary700.copy(alpha = 0.5f))
                    .border(1.dp, Secondary600, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = Primary300,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )

            Spacer(Modifier.height(24.dp))

            // Title
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                        append("Partagez vos\n")
                    }
                    withStyle(SpanStyle(color = Primary500, fontWeight = FontWeight.Bold)) {
                        append("recettes")
                    }
                    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                        append("\nfavorites")
                    }
                },
                fontSize = 36.sp,
                lineHeight = 42.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))

            // Subtitle
            Text(
                text = "Rejoignez une communauté passionnée. Découvrez de nouvelles saveurs et monétisez votre talent culinaire.",
                color = Secondary100,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(Modifier.height(32.dp))

            OutlinedButton(
                onClick = onExplorerClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Explorer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

// Features Section

@Composable
private fun FeaturesSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCream)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Title
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Secondary900)) {
                    append("Pourquoi ")
                }
                withStyle(SpanStyle(color = Primary500)) {
                    append("ForkEat")
                }
                withStyle(SpanStyle(color = Secondary900)) {
                    append(" ?")
                }
            },
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Tout ce dont vous avez besoin pour cuisiner.",
            color = Secondary600,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        // Feature cards
        FeatureCard(
            icon = Icons.Default.Share,
            iconBgColor = Secondary50,
            iconTint = Secondary800,
            title = "Partage Simple",
            description = "Publiez vos meilleures recettes en quelques clics et inspirez la communauté.",
        )

        Spacer(Modifier.height(16.dp))

        FeatureCard(
            icon = Icons.Default.MonetizationOn,
            iconBgColor = Primary100,
            iconTint = Primary500,
            title = "Monétisation",
            description = "Gagnez de l'argent grâce aux Super Likes et au soutien de vos fans.",
        )

        Spacer(Modifier.height(16.dp))

        FeatureCard(
            icon = Icons.Default.Group,
            iconBgColor = Orange50,
            iconTint = Orange500,
            title = "Communauté",
            description = "Échangez avec des chefs et amateurs passionnés du monde entier.",
        )
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    description: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Gray100),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Secondary900,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = description,
                color = Gray500,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )
        }
    }
}

// Footer

@Composable
private fun Footer() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Secondary900)
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = Primary500,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "ForkEat",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "© 2026 ForkEat. Fait avec passion.",
            color = Secondary200,
            fontSize = 12.sp,
        )
    }
}

// Enum pour les onglets de la NavBar
private enum class NavBarTab { HOME, SEARCH, PROFILE }

@Composable
private fun NavBar(
    selectedTab: NavBarTab,
    onTabSelected: (NavBarTab) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        NavigationBarItem(
            selected = selectedTab == NavBarTab.HOME,
            onClick = { onTabSelected(NavBarTab.HOME) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = "Accueil",
                    tint = if (selectedTab == NavBarTab.HOME) Primary500 else Secondary800
                )
            },
            label = { Text("Accueil") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Primary500,
                selectedTextColor = Primary500,
                unselectedIconColor = Secondary800,
                unselectedTextColor = Secondary800
            )
        )
        NavigationBarItem(
            selected = selectedTab == NavBarTab.SEARCH,
            onClick = { onTabSelected(NavBarTab.SEARCH) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Recherche",
                    tint = if (selectedTab == NavBarTab.SEARCH) Primary500 else Secondary800
                )
            },
            label = { Text("Recherche") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Primary500,
                selectedTextColor = Primary500,
                unselectedIconColor = Secondary800,
                unselectedTextColor = Secondary800
            )
        )
        NavigationBarItem(
            selected = selectedTab == NavBarTab.PROFILE,
            onClick = { onTabSelected(NavBarTab.PROFILE) },
            icon = {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profil",
                    tint = if (selectedTab == NavBarTab.PROFILE) Primary500 else Secondary800
                )
            },
            label = { Text("Profil") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Primary500,
                selectedTextColor = Primary500,
                unselectedIconColor = Secondary800,
                unselectedTextColor = Secondary800
            )
        )
    }
}

// Scaffold with top bar to reuse in other screens
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForkEatScaffold(
    navController: NavHostController? = null,
    isLoggedIn: Boolean = false,
    onLogout: () -> Unit = {},
    onNavigateToAccount: () -> Unit = { navController?.navigate("account") },
    showBars: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    // Observer la destination courante
    val navBackStackEntry = navController?.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.value?.destination?.route
    val selectedTab = when {
        currentRoute == "home" -> NavBarTab.HOME
        currentRoute == "recipes" || currentRoute == "recipe-form" ||
            currentRoute?.startsWith("recipes/") == true ||
            currentRoute?.startsWith("user/") == true -> NavBarTab.SEARCH
        currentRoute == "profile" || currentRoute == "account" ||
            currentRoute == "wallet" || currentRoute == "my-recipes" -> NavBarTab.PROFILE
        else -> NavBarTab.HOME
    }
    Scaffold(
        contentWindowInsets = if (showBars) ScaffoldDefaults.contentWindowInsets else WindowInsets(0),
        topBar = {
          if (showBars) {
            if (isLoggedIn) {
              ForkEatLoggedInTopBar(
                onNavigateToProfile = { navController?.navigate("profile") },
                onNavigateToAccount = onNavigateToAccount,
                onNavigateToWallet = { navController?.navigate("wallet") },
                onLogout = onLogout
              )
            } else {
              ForkEatTopBar(
                onNavigateToLogin = { navController?.navigate("login") },
                onNavigateToRegister = { navController?.navigate("register") },
              )
            }
          }
        },
        bottomBar = {
          if (showBars) {
            NavBar(
              selectedTab = selectedTab,
              onTabSelected = { tab ->
                val targetRoute = when (tab) {
                  NavBarTab.HOME -> "home"
                  NavBarTab.SEARCH -> "recipes"
                  NavBarTab.PROFILE -> "profile"
                }
                if (currentRoute != targetRoute) {
                  navController?.navigate(targetRoute)
                }
              }
            )
          }
        }
    ) { paddingValues -> content(paddingValues) }
}


// Preview

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(
        onNavigateToExplore = {},
    )
}
