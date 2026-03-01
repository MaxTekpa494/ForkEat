package fr.uge.android.forkeat.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import fr.uge.android.forkeat.designsystem.theme.*

@Composable
private fun AccountInfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Gray500)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = Secondary900, fontWeight = FontWeight.Medium)
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
            .clip(RoundedCornerShape(12.dp))
            .background(Gray100)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.titleMedium, color = Secondary900, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.bodySmall, color = Gray500)
        }
        Text(
            text = actionText,
            style = MaterialTheme.typography.bodyMedium,
            color = Primary500,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GoogleConnectionItem(onSetPassword: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Secondary50)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Secondary100),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Secondary700, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Connexion Google",
                style = MaterialTheme.typography.titleMedium,
                color = Secondary900,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Votre compte est connecté via Google. Définissez un mot de passe pour pouvoir également vous connecter avec votre email.",
                style = MaterialTheme.typography.bodySmall,
                color = Gray500,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onSetPassword,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Définir un mot de passe",
                    color = Primary500,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Modifier l'email",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Secondary900
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Secondary700)
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = onNewEmailChange,
                    label = { Text("Nouvel email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary500,
                        focusedLabelColor = Primary500
                    )
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = onCurrentPasswordChange,
                    label = { Text("Mot de passe actuel") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary500,
                        focusedLabelColor = Primary500
                    )
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler", color = Gray500)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onUpdateEmail,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary500)
                    ) {
                        Text("Modifier l'email", fontWeight = FontWeight.Bold)
                    }
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Confirmer le changement",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Secondary900
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Secondary700)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Un email vous a été envoyé. Entrez le code à 6 chiffres pour confirmer le changement de mot de passe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
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
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler", color = Gray500)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(code) },
                        enabled = code.length == 6 && !isLoading,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary500)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Valider", fontWeight = FontWeight.Bold)
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
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Gray100)
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
            cursorBrush = SolidColor(Secondary900),
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = char?.toString() ?: "",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Secondary900,
                        textAlign = TextAlign.Center
                    )
                    if (char == null) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(2.dp)
                                .background(Secondary300)
                        )
                    }
                }
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Définir un mot de passe",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Secondary900
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Secondary700)
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    label = { Text("Nouveau mot de passe") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary500,
                        focusedLabelColor = Primary500
                    )
                )
                Text(
                    "Minimum 8 caractères",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = onConfirmNewPasswordChange,
                    label = { Text("Confirmer le mot de passe") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary500,
                        focusedLabelColor = Primary500
                    )
                )
                errorMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler", color = Gray500)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onSetPassword,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary500)
                    ) {
                        Text("Définir le mot de passe", fontWeight = FontWeight.Bold)
                    }
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Modifier le mot de passe",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Secondary900
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Secondary700)
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = onCurrentPasswordChange,
                    label = { Text("Mot de passe actuel") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary500,
                        focusedLabelColor = Primary500
                    )
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    label = { Text("Nouveau mot de passe") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary500,
                        focusedLabelColor = Primary500
                    )
                )
                Text(
                    "Minimum 8 caractères",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = onConfirmNewPasswordChange,
                    label = { Text("Confirmer le nouveau mot de passe") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary500,
                        focusedLabelColor = Primary500
                    )
                )
                errorMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler", color = Gray500)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onUpdatePassword,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary500)
                    ) {
                        Text("Modifier le mot de passe", fontWeight = FontWeight.Bold)
                    }
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
            .background(SurfaceCream)
            .verticalScroll(rememberScrollState())
    ) {
        AccountHeader(uiState = uiState, onNavigateBack = onNavigateBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AccountInformationCard(uiState, accountViewModel)
            AccountSecurityCard(uiState, accountViewModel)
            AccountDangerZoneCard(accountViewModel)
            Spacer(Modifier.height(24.dp))
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

    if (uiState.resendMessage?.contains("envoyé") == true) {
        AlertDialog(
            onDismissRequest = accountViewModel::dismissResendMessage,
            title = { Text("Email envoyé !", fontWeight = FontWeight.Bold, color = Secondary900) },
            text = { Text("Un lien de confirmation vous a été envoyé. Vérifiez votre boîte mail pour activer votre adresse email.", color = Gray500) },
            confirmButton = {
                Button(
                    onClick = accountViewModel::dismissResendMessage,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary500),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}

@Composable
private fun AccountHeader(uiState: AccountUiState, onNavigateBack: () -> Unit) {
    val headerGradient = Brush.verticalGradient(
        colors = listOf(Secondary800, Secondary900)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerGradient)
            .padding(top = 16.dp, bottom = 40.dp, start = 8.dp, end = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }
            
            Spacer(Modifier.width(8.dp))
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Primary500)
                    .border(4.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${uiState.firstName.firstOrNull()?.uppercase() ?: ""}${uiState.lastName.firstOrNull()?.uppercase() ?: ""}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            
            Spacer(Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${uiState.firstName} ${uiState.lastName}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    lineHeight = 28.sp
                )
                Text(
                    text = "@${uiState.username}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Secondary100
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = uiState.role,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Secondary700.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .border(1.dp, Secondary600, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Primary300
                )
            }
        }
    }
}

@Composable
private fun AccountInformationCard(uiState: AccountUiState, viewModel: AccountViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Gray100)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Informations",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Secondary900
                )
                if (!uiState.isEditingInfo) {
                    IconButton(
                        onClick = viewModel::toggleEditInfo,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Primary50)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = Primary500, modifier = Modifier.size(18.dp))
                    }
                }
            }
            HorizontalDivider(color = Gray100, thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))

            if (!uiState.isEditingInfo) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AccountInfoRow(label = "Prénom", value = uiState.firstName)
                    AccountInfoRow(label = "Nom", value = uiState.lastName)
                    AccountInfoRow(label = "Nom d'utilisateur", value = "@${uiState.username}")
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Email", style = MaterialTheme.typography.labelSmall, color = Gray500)
                            if (uiState.emailVerified) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFDCFCE7), RoundedCornerShape(50))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "Vérifié",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF15803D),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(Orange50, RoundedCornerShape(50))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "Non vérifié",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Orange500,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(uiState.email, style = MaterialTheme.typography.bodyLarge, color = Secondary900, fontWeight = FontWeight.Medium)
                        if (!uiState.emailVerified) {
                            TextButton(
                                onClick = viewModel::resendConfirmation,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Primary500, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Renvoyer le lien de confirmation",
                                    color = Primary500,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            uiState.resendMessage?.let { message ->
                                if (!message.contains("envoyé")) {
                                    Text(
                                        message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = uiState.firstName,
                        onValueChange = viewModel::onFirstNameChange,
                        label = { Text("Prénom") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary500,
                            focusedLabelColor = Primary500
                        )
                    )
                    OutlinedTextField(
                        value = uiState.lastName,
                        onValueChange = viewModel::onLastNameChange,
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary500,
                            focusedLabelColor = Primary500
                        )
                    )
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = viewModel::onUsernameChange,
                        label = { Text("Nom d'utilisateur") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary500,
                            focusedLabelColor = Primary500
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = viewModel::toggleEditInfo) {
                            Text("Annuler", color = Gray500)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = viewModel::saveProfileInfo,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary500)
                        ) {
                            Text("Enregistrer", fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Gray100)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Secondary50),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Secondary700, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Sécurité",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Secondary900
                )
            }
            HorizontalDivider(color = Gray100, thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Primary100)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Primary50)
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Primary600)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Zone de danger",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Primary700
                )
            }
            HorizontalDivider(color = Primary100, thickness = 1.dp)
            Column(Modifier.padding(24.dp)) {
                Text(
                    "Supprimer mon compte",
                    style = MaterialTheme.typography.titleMedium,
                    color = Secondary900,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Cette action est irréversible. Toutes vos données seront définitivement supprimées de nos serveurs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = viewModel::deleteAccount,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary600),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Supprimer mon compte", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
