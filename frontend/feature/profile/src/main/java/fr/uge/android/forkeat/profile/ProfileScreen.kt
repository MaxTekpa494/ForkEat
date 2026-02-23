package fr.uge.android.forkeat.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.uge.android.forkeat.designsystem.theme.ForkEatTheme
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel()
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val newEmail by profileViewModel.newEmail.collectAsState()
    val currentPasswordEmailConfirm by profileViewModel.currentPasswordEmailConfirm.collectAsState()
    val currentPassword by profileViewModel.currentPassword.collectAsState()
    val newPassword by profileViewModel.newPassword.collectAsState()
    val confirmNewPassword by profileViewModel.confirmNewPassword.collectAsState()

  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(Color(0xFFF5F5F5))
          .verticalScroll(rememberScrollState())
  ) {
      // Profile Header Section
      ProfileHeader(uiState = uiState)

        // Main content area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileInformationCard(uiState, profileViewModel)
            SecurityCard(uiState, profileViewModel)
            MyStatisticsCard(uiState)
            MembershipLevelCard()
            ActiveSessionsCard()
            DangerZoneCard(profileViewModel)
        }
    }

    if (uiState.showEmailModal) {
        EmailModificationModal(
            newEmail = newEmail,
            onNewEmailChange = profileViewModel::onNewEmailChange,
            currentPassword = currentPasswordEmailConfirm,
            onCurrentPasswordChange = profileViewModel::onCurrentPasswordEmailConfirmChange,
            onUpdateEmail = profileViewModel::updateEmail,
            onDismiss = profileViewModel::closeEmailModal
        )
    }

    if (uiState.showPasswordModal) {
        PasswordModificationModal(
            currentPassword = currentPassword,
            onCurrentPasswordChange = profileViewModel::onCurrentPasswordChange,
            newPassword = newPassword,
            onNewPasswordChange = profileViewModel::onNewPasswordChange,
            confirmNewPassword = confirmNewPassword,
            onConfirmNewPasswordChange = profileViewModel::onConfirmNewPasswordChange,
            onUpdatePassword = profileViewModel::updatePassword,
            onDismiss = profileViewModel::closePasswordModal,
            errorMessage = uiState.error
        )
    }
}

@Composable
fun ProfileHeader(uiState: ProfileUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)) // Mimic from-secondary-800 to-secondary-900
                )
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF97316)), // Mimic bg-primary-500
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${uiState.firstName.firstOrNull()?.uppercase()}${uiState.lastName.firstOrNull()?.uppercase()}",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "${uiState.firstName} ${uiState.lastName}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "@${uiState.username}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFCBD5E1) // Mimic text-secondary-200
                )
                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    Text(
                        text = " Membre depuis ${uiState.memberSince.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8) // Mimic text-secondary-300
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = uiState.role,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .background(Color(0xFFF97316).copy(alpha = 0.2f), RoundedCornerShape(percent = 50))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color(0xFFF97316) // Mimic text-primary-300
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileInformationCard(uiState: ProfileUiState, profileViewModel: ProfileViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Informations personnelles",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1E293B)
                )
                TextButton(onClick = profileViewModel::toggleEditInfo) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = Color(0xFFF97316))
                    Spacer(Modifier.width(4.dp))
                    Text("Modifier", color = Color(0xFFF97316))
                }
            }
            Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)

            if (!uiState.isEditingInfo) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileInfoRow(label = "Prénom", value = uiState.firstName)
                    ProfileInfoRow(label = "Nom", value = uiState.lastName)
                    ProfileInfoRow(label = "Nom d'utilisateur", value = "@${uiState.username}")
                    ProfileInfoRow(label = "Email", value = uiState.email)
                }
            } else {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = uiState.firstName,
                        onValueChange = profileViewModel::onFirstNameChange,
                        label = { Text("Prénom") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.lastName,
                        onValueChange = profileViewModel::onLastNameChange,
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = profileViewModel::onUsernameChange,
                        label = { Text("Nom d'utilisateur") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = profileViewModel::toggleEditInfo) {
                            Text("Annuler")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = profileViewModel::saveProfileInfo) {
                            Text("Enregistrer")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SecurityCard(uiState: ProfileUiState, profileViewModel: ProfileViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF334155))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Sécurité",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1E293B)
                )
            }
            Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecurityItem(
                    label = "Adresse email",
                    value = uiState.email,
                    onClick = profileViewModel::openEmailModal
                )
                SecurityItem(
                    label = "Mot de passe",
                    value = "••••••••",
                    onClick = profileViewModel::openPasswordModal
                )
                SecurityItem(
                    label = "Authentification à deux facteurs",
                    value = "Sécurisez davantage votre compte",
                    actionText = "Activer",
                    onClick = profileViewModel::activateTwoFactorAuth
                )
            }
        }
    }
}

@Composable
fun SecurityItem(
    label: String,
    value: String,
    actionText: String = "Modifier",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.titleMedium, color = Color(0xFF1E293B))
            Text(value, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
        }
        Text(actionText, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFF97316), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ActiveSessionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Computer, contentDescription = null, tint = Color(0xFF334155))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Sessions actives",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1E293B)
                )
            }
            Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)
            Column(modifier = Modifier.padding(16.dp)) {
                // Placeholder for current session
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFECFDF5), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD1FAE5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DesktopWindows, contentDescription = null, tint = Color(0xFF059669))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Cette session (actuelle)", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1E293B))
                        Text("Paris, France", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4B5563))
                        Text("Dernière activité : il y a quelques secondes", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                }
            }
        }
    }
}

@Composable
fun DangerZoneCard(profileViewModel: ProfileViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFFEE2E2)) // Mimic border-red-200
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF2F2)) // Mimic bg-red-50
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Zone de danger",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFEF4444) // Mimic text-red-900
                )
            }
            Divider(color = Color(0xFFFEE2E2), thickness = 1.dp)
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Supprimer mon compte", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1E293B))
                    Text(
                        "Cette action est irréversible. Toutes vos données seront définitivement supprimées.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = profileViewModel::deleteAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Supprimer", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun MyStatisticsCard(uiState: ProfileUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Mes statistiques",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            StatisticItem(
                icon = Icons.Default.Book,
                iconTint = Color(0xFF1E293B),
                label = "Recettes",
                value = uiState.totalRecipes.toString()
            )
            StatisticItem(
                icon = Icons.Default.Favorite,
                iconTint = Color(0xFFEF4444),
                label = "Likes reçus",
                value = uiState.totalLikes.toString()
            )
            StatisticItem(
                icon = Icons.Default.Group,
                iconTint = Color(0xFF3B82F6),
                label = "Abonnés",
                value = uiState.followers.toString()
            )
            StatisticItem(
                icon = Icons.Default.Star,
                iconTint = Color(0xFFF97316),
                label = "Super Likes",
                value = uiState.superLikes.toString()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { /* TODO: Navigate to My Recipes */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Voir mes recettes", color = Color(0xFF1E293B))
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF1E293B))
            }
        }
    }
}

@Composable
fun StatisticItem(icon: ImageVector, iconTint: Color, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconTint.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4B5563))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF1E293B))
        }
    }
}

@Composable
fun MembershipLevelCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(colors = listOf(Color(0xFFF97316), Color(0xFFEA580C))), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text("Membre Bronze", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Text(
                "Continuez à partager vos recettes pour débloquer le niveau suivant !",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LinearProgressIndicator(
                progress = 0.25f, // Placeholder progress
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
            Text(
                "0 / 10 recettes pour Argent",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun QuickActionsProfileCard(profileViewModel: ProfileViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Actions rapides",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            QuickActionItem(
                icon = Icons.Default.Home,
                label = "Dashboard",
                onClick = profileViewModel::navigateToDashboard
            )
            QuickActionItem(
                icon = Icons.Default.Wallet,
                label = "Mon wallet",
                onClick = profileViewModel::navigateToWallet
            )
            QuickActionItem(
                icon = Icons.Default.Add,
                label = "Créer une recette",
                onClick = profileViewModel::navigateToCreateRecipe
            )
        }
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF4B5563), modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = Color(0xFF1E293B))
    }
}


@Composable
fun EmailModificationModal(
    newEmail: String,
    onNewEmailChange: (String) -> Unit,
    currentPassword: String,
    onCurrentPasswordChange: (String) -> Unit,
    onUpdateEmail: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Modifier l'email",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E293B)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = onNewEmailChange,
                    label = { Text("Nouvel email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = onCurrentPasswordChange,
                    label = { Text("Mot de passe actuel (pour confirmation)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onUpdateEmail) {
                        Text("Modifier l'email")
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordModificationModal(
    currentPassword: String,
    onCurrentPasswordChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmNewPassword: String,
    onConfirmNewPasswordChange: (String) -> Unit,
    onUpdatePassword: () -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String?
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Modifier le mot de passe",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E293B)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = onCurrentPasswordChange,
                    label = { Text("Mot de passe actuel") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    label = { Text("Nouveau mot de passe") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Minimum 8 caractères",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(start = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = onConfirmNewPasswordChange,
                    label = { Text("Confirmer le nouveau mot de passe") },
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onUpdatePassword) {
                        Text("Modifier le mot de passe")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileGuestScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = "Icône invité",
                tint = Color(0xFF334155),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Bienvenue sur ForkEat",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Connecte-toi ou crée un compte pour accéder à toutes les fonctionnalités.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF334155),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Se connecter")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("S'inscrire")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewProfileScreen() {
    ForkEatTheme {
        ProfileScreen()
    }
}
