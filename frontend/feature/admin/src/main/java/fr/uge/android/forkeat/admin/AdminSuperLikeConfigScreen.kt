package fr.uge.android.forkeat.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSuperLikeConfigScreen(
    onBack: () -> Unit,
    viewModel: AdminSuperLikeConfigViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var priceStr by remember { mutableStateOf("") }
    var ratioStr by remember { mutableStateOf("") }

    LaunchedEffect(uiState.config) {
        uiState.config?.let {
            priceStr = it.priceCents.toString()
            ratioStr = String.format("%.2f", it.earningsRatio)
        }
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            snackbarHostState.showSnackbar("Configuration sauvegardée")
            viewModel.dismissSuccess()
        }
    }

    uiState.error?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Erreur") },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("OK") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Config Super-Like", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = AdminPurple300)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminPurple900)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AdminPurple500)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AdminPurple100)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Valeurs actuelles ─────────────────────────────────────────────
            uiState.config?.let { cfg ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Configuration actuelle", fontWeight = FontWeight.Bold, color = AdminPurple900)
                    Spacer(Modifier.height(4.dp))
                    Text("Prix : ${String.format("%.2f€", cfg.priceCents / 100.0)}", color = AdminPurple700, fontSize = 14.sp)
                    Text("Ratio redistribution : ${String.format("%.0f", cfg.earningsRatio * 100)}%", color = AdminPurple700, fontSize = 14.sp)
                    Text("Ratio plateforme : ${String.format("%.0f", (1.0 - cfg.earningsRatio) * 100)}%", color = AdminPurple700, fontSize = 14.sp)
                    cfg.updatedAt?.let {
                        Text("Dernière MAJ : ${it.take(16).replace("T", " ")}", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Text("Modifier la configuration", fontWeight = FontWeight.SemiBold, color = AdminPurple900, fontSize = 16.sp)

            OutlinedTextField(
                value = priceStr, onValueChange = { priceStr = it },
                label = { Text("Prix du super-like (centimes)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ratioStr, onValueChange = { ratioStr = it },
                label = { Text("Ratio redistribution (ex: 0.40)") },
                supportingText = { Text("Entre 0.01 et 0.99 — part reversée aux créateurs") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val price = priceStr.toLongOrNull() ?: return@Button
                    val ratio = ratioStr.toDoubleOrNull() ?: return@Button
                    viewModel.save(price, ratio)
                },
                enabled = !uiState.isSaving && priceStr.isNotBlank() && ratioStr.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AdminPurple600)
            ) {
                if (uiState.isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.padding(4.dp))
                else Text("Enregistrer", color = Color.White)
            }
        }
    }
}
