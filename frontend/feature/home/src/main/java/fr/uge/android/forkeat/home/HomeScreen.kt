package fr.uge.android.forkeat.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
    onNavigateToLogin: () -> Unit = {},
    onNavigateToExplore: () -> Unit = {},
) {
    Scaffold(
        topBar = { ForkEatTopBar(onNavigateToLogin = onNavigateToLogin) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            HeroSection(
                onCommencerClick = onNavigateToLogin,
                onExplorerClick = onNavigateToExplore,
            )
            FeaturesSection()
            Footer()
        }
    }
}

// Top Bar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForkEatTopBar(
    onNavigateToLogin: () -> Unit,
    showSearchIcon: Boolean = false,
    onSearchIconClick: (() -> Unit)? = null,
    onNavigateToRegister: () -> Unit

) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
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
        },
        actions = {
            if (showSearchIcon) {
                IconButton(onClick = { onSearchIconClick?.invoke() }) {
                    Icon(Icons.Default.Search, contentDescription = "Rechercher", tint = Secondary800)
                }
            }
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

// Hero Section

@Composable
private fun HeroSection(
    onCommencerClick: () -> Unit,
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
                text = "Rejoignez une communaut\u00e9 passionn\u00e9e. D\u00e9couvrez de nouvelles saveurs et mon\u00e9tisez votre talent culinaire.",
                color = Secondary100,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(Modifier.height(32.dp))

            // CTA buttons
            Button(
                onClick = onCommencerClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary500,
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
            ) {
                Icon(
                    Icons.Default.RocketLaunch,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Commencer",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

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
            .padding(horizontal = 24.dp, vertical = 48.dp),
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
            description = "Publiez vos meilleures recettes en quelques clics et inspirez la communaut\u00e9.",
        )

        Spacer(Modifier.height(16.dp))

        FeatureCard(
            icon = Icons.Default.MonetizationOn,
            iconBgColor = Primary100,
            iconTint = Primary500,
            title = "Mon\u00e9tisation",
            description = "Gagnez de l'argent gr\u00e2ce aux Super Likes et au soutien de vos fans.",
        )

        Spacer(Modifier.height(16.dp))

        FeatureCard(
            icon = Icons.Default.Group,
            iconBgColor = Orange50,
            iconTint = Orange500,
            title = "Communaut\u00e9",
            description = "\u00c9changez avec des chefs et amateurs passionn\u00e9s du monde entier.",
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
            text = "\u00a9 2026 ForkEat. Fait avec passion.",
            color = Secondary200,
            fontSize = 12.sp,
        )
    }
}

// Scaffold with top bar to reuse in other screens
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForkEatScaffold(
    navController: NavHostController? = null,
    title: String = "ForkEat",
    content: @Composable (PaddingValues) -> Unit,
    showSearchIcon: Boolean = false,
    onSearchIconClick: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            ForkEatTopBar(
                onNavigateToLogin = {
                    navController?.navigate("login")
                },
                showSearchIcon = showSearchIcon,
                onSearchIconClick = onSearchIconClick
            )
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}


// Preview

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen()
}
