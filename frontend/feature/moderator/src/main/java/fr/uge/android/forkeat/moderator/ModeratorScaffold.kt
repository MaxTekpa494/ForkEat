package fr.uge.android.forkeat.moderator

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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Restaurant
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

// ── Couleurs moderateur (orange) ─────────────────────────────────────────────
val ModeratorOrange900 = Color(0xFF7C2D12)
val ModeratorOrange800 = Color(0xFF9A3412)
val ModeratorOrange700 = Color(0xFFC2410C)
val ModeratorOrange600 = Color(0xFFEA580C)
val ModeratorOrange500 = Color(0xFFF97316)
val ModeratorOrange300 = Color(0xFFFDBA74)
val ModeratorOrange200 = Color(0xFFFED7AA)
val ModeratorOrange100 = Color(0xFFFFEDD5)

private enum class ModeratorTab { RECIPES, REPORTS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeratorScaffold(
    currentRoute: String,
    onNavigateToRecipes: () -> Unit,
    onNavigateToReports: () -> Unit,
    onExit: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val selectedTab = when (currentRoute) {
        "moderator-recipes" -> ModeratorTab.RECIPES
        "moderator-reports" -> ModeratorTab.REPORTS
        else              -> ModeratorTab.RECIPES
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
                                .background(ModeratorOrange700),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = ModeratorOrange200,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                                    append("Fork")
                                }
                                withStyle(SpanStyle(color = ModeratorOrange300, fontWeight = FontWeight.Bold)) {
                                    append("Eat")
                                }
                            },
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Modérateur",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ModeratorOrange300,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .background(ModeratorOrange700, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onExit) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Quitter le mode modérateur",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ModeratorOrange900
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ModeratorOrange900,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == ModeratorTab.RECIPES,
                    onClick = { if (selectedTab != ModeratorTab.RECIPES) onNavigateToRecipes() },
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Recettes") },
                    label = { Text("Recettes", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ModeratorOrange300,
                        selectedTextColor = ModeratorOrange300,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = ModeratorOrange700
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == ModeratorTab.REPORTS,
                    onClick = { if (selectedTab != ModeratorTab.REPORTS) onNavigateToReports() },
                    icon = { Icon(Icons.Default.Report, contentDescription = "Signalements") },
                    label = { Text("Signalements", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ModeratorOrange300,
                        selectedTextColor = ModeratorOrange300,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = ModeratorOrange700
                    )
                )
            }
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}
