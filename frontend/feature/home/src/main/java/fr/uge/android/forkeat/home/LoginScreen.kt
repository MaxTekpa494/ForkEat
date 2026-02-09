package fr.uge.android.forkeat.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.uge.android.forkeat.designsystem.theme.Gray500
import fr.uge.android.forkeat.designsystem.theme.Primary500
import fr.uge.android.forkeat.designsystem.theme.Primary600
import fr.uge.android.forkeat.designsystem.theme.Secondary100
import fr.uge.android.forkeat.designsystem.theme.Secondary700
import fr.uge.android.forkeat.designsystem.theme.Secondary800
import fr.uge.android.forkeat.designsystem.theme.Secondary900
import fr.uge.android.forkeat.designsystem.theme.SurfaceCream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    newUser: Boolean = false,
    viewModel: LoginViewModel = viewModel(),
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Secondary800,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceCream,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceCream)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            // Header
            if(newUser){
                Text(
                    text = "Vous êtes maintenant inscrit !",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Secondary900,
                )
            }else{
                Text(
                    text = "Bon retour !",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Secondary900,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Connectez-vous pour retrouver vos recettes",
                fontSize = 14.sp,
                color = Gray500,
            )

            // Error message
            if (uiState.errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFFEF2F2),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = Color(0xFFDC2626),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Google OAuth2 button
            OutlinedButton(
                onClick = { /* TODO: Google OAuth2 */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, Color(0xFFE5E7EB)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Secondary800,
                ),
            ) {
                Text(
                    "G",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF4285F4),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Continuer avec Google",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            // Separator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFD1D5DB))
                Text(
                    text = "Ou avec votre email",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Gray500,
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFD1D5DB))
            }

            Spacer(Modifier.height(24.dp))

            // Username field
            Text(
                text = "Email ou nom d'utilisateur",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Secondary900,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    viewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("chef@forkeat.com ou @chef123", color = Color(0xFF9CA3AF))
                },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF9CA3AF))
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedBorderColor = Primary500,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                ),
                singleLine = true,
                enabled = !uiState.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(Modifier.height(20.dp))

            // Password field
            Text(
                text = "Mot de passe",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Secondary900,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022", color = Color(0xFF9CA3AF))
                },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF9CA3AF))
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Masquer" else "Afficher",
                            tint = Color(0xFF9CA3AF),
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedBorderColor = Primary500,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                ),
                singleLine = true,
                enabled = !uiState.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            )

            Spacer(Modifier.height(16.dp))

            // Remember me + Forgot password
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Primary500,
                        uncheckedColor = Color(0xFFD1D5DB),
                    ),
                )
                Text(
                    text = "Se souvenir de moi",
                    fontSize = 14.sp,
                    color = Secondary900,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onForgotPassword() }) {
                    Text(
                        "Mot de passe oubli\u00e9 ?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Primary600,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Login button
            Button(
                onClick = { viewModel.login(username, password) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary500,
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    "Se connecter",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            Spacer(Modifier.height(32.dp))

            // Separator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFD1D5DB))
                Text(
                    text = "Pas encore de compte ?",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    fontSize = 12.sp,
                    color = Gray500,
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFD1D5DB))
            }

            Spacer(Modifier.height(20.dp))

            // Register button
            OutlinedButton(
                onClick = onNavigateToRegister,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                border = BorderStroke(2.dp, Secondary100),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Secondary700,
                ),
            ) {
                Text(
                    "Cr\u00e9er un compte gratuitement",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    LoginScreen()
}
