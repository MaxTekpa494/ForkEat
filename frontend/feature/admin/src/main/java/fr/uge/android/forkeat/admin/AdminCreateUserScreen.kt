package fr.uge.android.forkeat.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AdminCreateUserScreen(
    currentRoute: String,
    onNavigateToDashboard: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onNavigateToWallets: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminCreateUserViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var username   by remember { mutableStateOf("") }
    var firstName  by remember { mutableStateOf("") }
    var lastName   by remember { mutableStateOf("") }
    var email      by remember { mutableStateOf("") }
    var password   by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var passwordMismatch by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(AdminCreateRole.MODERATOR) }

    // Réinitialiser le formulaire après succès
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            username = ""
            firstName = ""
            lastName = ""
            email = ""
            password = ""
            confirmPassword = ""
            passwordMismatch = false
        }
    }

    AdminScaffold(
        currentRoute = currentRoute,
        onNavigateToDashboard = onNavigateToDashboard,
        onNavigateToUsers = onNavigateToUsers,
        onNavigateToRecipes = onNavigateToRecipes,
        onNavigateToWallets = onNavigateToWallets,
        onNavigateToCreate = {},
        onLogout = onLogout
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F3FF))
                .verticalScroll(rememberScrollState())
        ) {
            // En-tête
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AdminPurple900)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column {
                    Text("Créer un compte", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    Spacer(Modifier.height(2.dp))
                    Text("Modérateur ou administrateur", fontSize = 12.sp, color = AdminPurple300)
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Sélecteur de rôle
                Text("Rôle", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AdminPurple900)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoleChip(
                        label = "Modérateur",
                        icon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        selected = selectedRole == AdminCreateRole.MODERATOR,
                        selectedColor = Color(0xFF0891B2),
                        selectedBg = Color(0xFFE0F2FE),
                        onClick = { selectedRole = AdminCreateRole.MODERATOR; viewModel.clearStatus() }
                    )
                    RoleChip(
                        label = "Administrateur",
                        icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        selected = selectedRole == AdminCreateRole.ADMIN,
                        selectedColor = AdminPurple700,
                        selectedBg = AdminPurple100,
                        onClick = { selectedRole = AdminCreateRole.ADMIN; viewModel.clearStatus() }
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Champs du formulaire
                AdminFormField(
                    label = "Nom d'utilisateur",
                    value = username,
                    onValueChange = { username = it; viewModel.clearStatus() },
                    placeholder = "chef_dupont",
                    keyboardType = KeyboardType.Text
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        AdminFormField(
                            label = "Prénom",
                            value = firstName,
                            onValueChange = { firstName = it; viewModel.clearStatus() },
                            placeholder = "Jean"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AdminFormField(
                            label = "Nom",
                            value = lastName,
                            onValueChange = { lastName = it; viewModel.clearStatus() },
                            placeholder = "Dupont"
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                AdminFormField(
                    label = "Email",
                    value = email,
                    onValueChange = { email = it; viewModel.clearStatus() },
                    placeholder = "jean.dupont@forkeat.com",
                    keyboardType = KeyboardType.Email
                )
                Spacer(Modifier.height(12.dp))

                // Mot de passe avec toggle visibilité
                Text("Mot de passe", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AdminPurple900)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; viewModel.clearStatus() },
                    placeholder = { Text("Min. 8 caractères", color = Color(0xFF9CA3AF), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedBorderColor = AdminPurple500,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    ),
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(Modifier.height(12.dp))

                // Confirmation du mot de passe
                Text("Confirmer le mot de passe", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AdminPurple900)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        passwordMismatch = false
                        viewModel.clearStatus()
                    },
                    placeholder = { Text("Répéter le mot de passe", color = Color(0xFF9CA3AF), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = if (passwordMismatch) Color(0xFFDC2626) else Color(0xFF9CA3AF), modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = passwordMismatch,
                    supportingText = if (passwordMismatch) {
                        { Text("Les mots de passe ne correspondent pas", color = Color(0xFFDC2626), fontSize = 11.sp) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedBorderColor = AdminPurple500,
                        errorBorderColor = Color(0xFFDC2626),
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    ),
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )

                Spacer(Modifier.height(20.dp))

                // Feedback erreur
                if (uiState.error != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = uiState.error!!, color = Color(0xFFDC2626), fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Feedback succès
                if (uiState.isSuccess && uiState.successMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = uiState.successMessage!!, color = Color(0xFF16A34A), fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Bouton créer
                Button(
                    onClick = {
                        if (password != confirmPassword) {
                            passwordMismatch = true
                        } else {
                            passwordMismatch = false
                            viewModel.createUser(username.trim(), firstName.trim(), lastName.trim(), email.trim(), password, selectedRole)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdminPurple700,
                        contentColor = Color.White
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = if (selectedRole == AdminCreateRole.MODERATOR) "Créer le modérateur" else "Créer l'administrateur",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleChip(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    selectedColor: Color,
    selectedBg: Color,
    onClick: () -> Unit
) {
    val bgColor    = if (selected) selectedBg else Color.White
    val textColor  = if (selected) selectedColor else Color(0xFF6B7280)
    val borderColor = if (selected) selectedColor else Color(0xFFE5E7EB)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(16.dp)) { icon() }
        Spacer(Modifier.width(6.dp))
        Text(text = label, color = textColor, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
    }
}

@Composable
private fun AdminFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AdminPurple900)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color(0xFF9CA3AF), fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFE5E7EB),
            focusedBorderColor = AdminPurple500,
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next)
    )
}
