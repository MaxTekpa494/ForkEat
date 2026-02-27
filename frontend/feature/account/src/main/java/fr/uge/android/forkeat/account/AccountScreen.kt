package fr.uge.android.forkeat.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
private fun AccountInfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SecurityItem(
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
private fun GoogleConnectionItem(onSetPassword: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F9FF), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFDBEAFE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Connexion Google",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1E293B)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Votre compte est connecté via Google. Définissez un mot de passe pour pouvoir également vous connecter avec votre email.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280)
            )
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onSetPassword,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Définir un mot de passe",
                    color = Color(0xFFF97316),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EmailModificationModal(
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
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onUpdateEmail) { Text("Modifier l'email") }
                }
            }
        }
    }
}

@Composable
private fun ConfirmPasswordChangeModal(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?
) {
    var code by remember { mutableStateOf("") }

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
                        "Confirmer le changement",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E293B)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Un email vous a été envoyé. Entrez le code à 6 chiffres pour confirmer le changement de mot de passe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
                Spacer(Modifier.height(24.dp))
                PasswordConfirmCodeField(
                    code = code,
                    onCodeChange = { code = it }
                )
                errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(code) },
                        enabled = code.length == 6 && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Valider")
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordConfirmCodeField(
    codeLength: Int = 6,
    code: String,
    onCodeChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = code,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }
                if (filtered.length <= codeLength) onCodeChange(filtered)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = SolidColor(Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(codeLength) { index ->
                val char = code.getOrNull(index)
                Text(
                    text = char?.toString() ?: "·",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (char != null) Color(0xFF1E293B) else Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SetPasswordModal(
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmNewPassword: String,
    onConfirmNewPasswordChange: (String) -> Unit,
    onSetPassword: () -> Unit,
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
                        "Définir un mot de passe",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E293B)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }
                Spacer(Modifier.height(16.dp))
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
                    label = { Text("Confirmer le mot de passe") },
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
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onSetPassword) { Text("Définir le mot de passe") }
                }
            }
        }
    }
}

@Composable
private fun PasswordModificationModal(
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
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onUpdatePassword) { Text("Modifier le mot de passe") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    accountViewModel: AccountViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by accountViewModel.uiState.collectAsState()
    val newEmail by accountViewModel.newEmail.collectAsState()
    val currentPasswordEmailConfirm by accountViewModel.currentPasswordEmailConfirm.collectAsState()
    val currentPassword by accountViewModel.currentPassword.collectAsState()
    val newPassword by accountViewModel.newPassword.collectAsState()
    val confirmNewPassword by accountViewModel.confirmNewPassword.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
    ) {
        AccountHeader(uiState = uiState, onNavigateBack = onNavigateBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AccountInformationCard(uiState, accountViewModel)
            AccountSecurityCard(uiState, accountViewModel)
            AccountDangerZoneCard(accountViewModel)
        }
    }

    if (uiState.showEmailModal) {
        EmailModificationModal(
            newEmail = newEmail,
            onNewEmailChange = accountViewModel::onNewEmailChange,
            currentPassword = currentPasswordEmailConfirm,
            onCurrentPasswordChange = accountViewModel::onCurrentPasswordEmailConfirmChange,
            onUpdateEmail = accountViewModel::updateEmail,
            onDismiss = accountViewModel::closeEmailModal
        )
    }

    if (uiState.showPasswordModal) {
        PasswordModificationModal(
            currentPassword = currentPassword,
            onCurrentPasswordChange = accountViewModel::onCurrentPasswordChange,
            newPassword = newPassword,
            onNewPasswordChange = accountViewModel::onNewPasswordChange,
            confirmNewPassword = confirmNewPassword,
            onConfirmNewPasswordChange = accountViewModel::onConfirmNewPasswordChange,
            onUpdatePassword = accountViewModel::updatePassword,
            onDismiss = accountViewModel::closePasswordModal,
            errorMessage = uiState.error
        )
    }

    if (uiState.showSetPasswordModal) {
        SetPasswordModal(
            newPassword = newPassword,
            onNewPasswordChange = accountViewModel::onNewPasswordChange,
            confirmNewPassword = confirmNewPassword,
            onConfirmNewPasswordChange = accountViewModel::onConfirmNewPasswordChange,
            onSetPassword = accountViewModel::setPassword,
            onDismiss = accountViewModel::closeSetPasswordModal,
            errorMessage = uiState.error
        )
    }

    if (uiState.showConfirmPasswordModal) {
        ConfirmPasswordChangeModal(
            onConfirm = { code -> accountViewModel.confirmPasswordChange(code) },
            onDismiss = { accountViewModel.closeConfirmPasswordModal() },
            isLoading = uiState.isLoading,
            errorMessage = uiState.error
        )
    }
}

@Composable
private fun AccountHeader(uiState: AccountUiState, onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF97316)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${uiState.firstName.firstOrNull()?.uppercase() ?: ""}${uiState.lastName.firstOrNull()?.uppercase() ?: ""}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "Mon compte",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "@${uiState.username}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFCBD5E1)
                )
                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = uiState.role,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .background(Color(0xFFF97316).copy(alpha = 0.2f), RoundedCornerShape(percent = 50))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color(0xFFF97316)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountInformationCard(uiState: AccountUiState, viewModel: AccountViewModel) {
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
                TextButton(onClick = viewModel::toggleEditInfo) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = Color(0xFFF97316))
                    Spacer(Modifier.width(4.dp))
                    Text("Modifier", color = Color(0xFFF97316))
                }
            }
            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)

            if (!uiState.isEditingInfo) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountInfoRow(label = "Prénom", value = uiState.firstName)
                    AccountInfoRow(label = "Nom", value = uiState.lastName)
                    AccountInfoRow(label = "Nom d'utilisateur", value = "@${uiState.username}")
                    AccountInfoRow(label = "Email", value = uiState.email)
                }
            } else {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = uiState.firstName,
                        onValueChange = viewModel::onFirstNameChange,
                        label = { Text("Prénom") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.lastName,
                        onValueChange = viewModel::onLastNameChange,
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = viewModel::onUsernameChange,
                        label = { Text("Nom d'utilisateur") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = viewModel::toggleEditInfo) {
                            Text("Annuler")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = viewModel::saveProfileInfo) {
                            Text("Enregistrer")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSecurityCard(uiState: AccountUiState, viewModel: AccountViewModel) {
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
            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecurityItem(
                    label = "Adresse email",
                    value = uiState.email,
                    onClick = viewModel::openEmailModal
                )
                if (uiState.authMode == "GOOGLE") {
                    GoogleConnectionItem(onSetPassword = viewModel::openSetPasswordModal)
                } else {
                    SecurityItem(
                        label = "Mot de passe",
                        value = "••••••••",
                        onClick = viewModel::openPasswordModal
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountDangerZoneCard(viewModel: AccountViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFFEE2E2))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF2F2))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Zone de danger",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFEF4444)
                )
            }
            HorizontalDivider(color = Color(0xFFFEE2E2), thickness = 1.dp)
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
                    onClick = viewModel::deleteAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Supprimer", color = Color.White)
                }
            }
        }
    }
}
