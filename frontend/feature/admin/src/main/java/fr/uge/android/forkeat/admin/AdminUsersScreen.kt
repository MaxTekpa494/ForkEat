package fr.uge.android.forkeat.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.uge.android.forkeat.network.dto.UserResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    currentRoute: String,
    initialTab: Int = 0,
    onNavigateToDashboard: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onNavigateToWallets: () -> Unit,
    onNavigateToReports : () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToPromotions: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminUsersViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    val tabs = listOf(
        "Membres (${uiState.members.size})",
        "Modérateurs (${uiState.moderators.size})",
        "Admins (${uiState.admins.size})"
    )
    val currentList = when (selectedTab) {
        0 -> uiState.members
        1 -> uiState.moderators
        else -> uiState.admins
    }

    AdminScaffold(
        currentRoute = currentRoute,
        onNavigateToDashboard = onNavigateToDashboard,
        onNavigateToUsers = {},
        onNavigateToRecipes = onNavigateToRecipes,
        onNavigateToWallets = onNavigateToWallets,
        onNavigateToCreate = onNavigateToCreate,
        onNavigateToPromotions = onNavigateToPromotions,
        onNavigateToReports = onNavigateToReports,
        onLogout = onLogout
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadUsers() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F3FF))
            ) {
                // En-tête
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AdminPurple900)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Utilisateurs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }

                // Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = AdminPurple900,
                    contentColor = AdminPurple300,
                    edgePadding = 0.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) AdminPurple300 else Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }

                if (uiState.error != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = uiState.error!!, color = Color(0xFFDC2626), fontSize = 13.sp)
                    }
                }

                if (currentList.isEmpty() && !uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = AdminPurple300,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Aucun utilisateur",
                                color = Color(0xFF6B7280),
                                fontSize = 15.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                    ) {
                        items(currentList) { user ->
                            UserListItem(user = user, isModerator = selectedTab == 1, isAdmin = selectedTab == 2)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserListItem(user: UserResource, isModerator: Boolean, isAdmin: Boolean = false) {
    val avatarBg = when {
        isAdmin    -> AdminPurple200
        isModerator -> Color(0xFFE0F2FE)
        else       -> AdminPurple100
    }
    val avatarText = when {
        isAdmin    -> AdminPurple700
        isModerator -> Color(0xFF0891B2)
        else       -> AdminPurple600
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = avatarText
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(Modifier.width(6.dp))
                    when {
                        isAdmin -> Box(
                            modifier = Modifier
                                .background(AdminPurple200, RoundedCornerShape(50))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = AdminPurple700, modifier = Modifier.size(10.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Admin", fontSize = 9.sp, color = AdminPurple700, fontWeight = FontWeight.Bold)
                            }
                        }
                        isModerator -> Box(
                            modifier = Modifier
                                .background(Color(0xFFE0F2FE), RoundedCornerShape(50))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF0891B2), modifier = Modifier.size(10.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Modérateur", fontSize = 9.sp, color = Color(0xFF0891B2), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(text = "@${user.username}", fontSize = 12.sp, color = Color(0xFF6B7280))
                Text(text = user.email, fontSize = 12.sp, color = Color(0xFF9CA3AF))
            }
        }
    }
}
