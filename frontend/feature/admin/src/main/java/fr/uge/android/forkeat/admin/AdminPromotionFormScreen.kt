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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPromotionFormScreen(
    promotionId: String?,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AdminPromotionFormViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEditMode = promotionId != null

    LaunchedEffect(promotionId) { viewModel.load(promotionId) }
    LaunchedEffect(uiState.success) { if (uiState.success) onSuccess() }

    var name     by remember { mutableStateOf("") }
    // dates stockées en millis UTC pour les pickers, null = non sélectionnée
    var startsAtMs  by remember { mutableStateOf<Long?>(null) }
    var startsHour  by remember { mutableStateOf(9) }
    var startsMin   by remember { mutableStateOf(0) }
    var endsAtMs    by remember { mutableStateOf<Long?>(null) }
    var endsHour    by remember { mutableStateOf(23) }
    var endsMin     by remember { mutableStateOf(59) }
    var priceStr by remember { mutableStateOf("") }
    var bonusStr by remember { mutableStateOf("") }

    // Pickers visibles
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker   by remember { mutableStateOf(false) }
    var showEndTimePicker   by remember { mutableStateOf(false) }

    // Pré-remplir en mode édition
    LaunchedEffect(uiState.promotion) {
        uiState.promotion?.let { p ->
            name = p.name
            priceStr = p.priceCents.toString()
            bonusStr = p.bonusEveryN?.toString() ?: ""
            p.startsAt?.let { iso ->
                val zdt = ZonedDateTime.parse(iso).withZoneSameInstant(ZoneId.systemDefault())
                startsAtMs = zdt.toInstant().toEpochMilli()
                startsHour = zdt.hour; startsMin = zdt.minute
            }
            p.endsAt?.let { iso ->
                val zdt = ZonedDateTime.parse(iso).withZoneSameInstant(ZoneId.systemDefault())
                endsAtMs = zdt.toInstant().toEpochMilli()
                endsHour = zdt.hour; endsMin = zdt.minute
            }
        }
    }

    // Simulation live — calculée directement dans la recomposition (pas de derivedStateOf
    // sur des valeurs non-State, ce qui causerait un résultat figé)
    val earningsRatio = uiState.config?.earningsRatio ?: 0.0
    val simulation = run {
        val n = bonusStr.toIntOrNull() ?: return@run null
        val price = priceStr.toLongOrNull() ?: return@run null
        if (n < 2 || price <= 0 || earningsRatio <= 0.0) return@run null
        val isProfitable = n * earningsRatio > (1.0 - earningsRatio)
        val netProfit = (n * earningsRatio - (1.0 - earningsRatio)) * price / 100.0
        val minN = ceil((1.0 - earningsRatio) / earningsRatio).toInt() + 1
        Triple(isProfitable, netProfit, minN)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    fun formatDisplay(epochMs: Long?, hour: Int, min: Int): String {
        if (epochMs == null) return ""
        val zdt = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
            .withHour(hour).withMinute(min).withSecond(0)
        return zdt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    }

    fun toIso(epochMs: Long, hour: Int, min: Int): String {
        val zdt = Instant.ofEpochMilli(epochMs).atZone(ZoneId.of("UTC"))
            .withHour(hour).withMinute(min).withSecond(0).withNano(0)
        return zdt.format(DateTimeFormatter.ISO_INSTANT)
    }

    // ── Pickers ──────────────────────────────────────────────────────────────
    if (showStartDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = startsAtMs)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startsAtMs = dpState.selectedDateMillis
                    showStartDatePicker = false
                    showStartTimePicker = true
                }) { Text("Suivant") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Annuler") } }
        ) { DatePicker(state = dpState) }
    }

    if (showStartTimePicker) {
        var tempHour by remember { mutableStateOf(startsHour) }
        var tempMin  by remember { mutableStateOf(startsMin) }
        Dialog(onDismissRequest = { showStartTimePicker = false }) {
            Column(
                modifier = Modifier.background(Color.White, RoundedCornerShape(16.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Heure de début", fontWeight = FontWeight.Bold, color = AdminPurple900, modifier = Modifier.padding(bottom = 16.dp))
                WheelTimePicker(
                    initialHour = startsHour,
                    initialMinute = startsMin,
                    onTimeSelected = { h, m -> tempHour = h; tempMin = m }
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showStartTimePicker = false }) { Text("Annuler") }
                    TextButton(onClick = {
                        startsHour = tempHour; startsMin = tempMin
                        showStartTimePicker = false
                    }) { Text("OK") }
                }
            }
        }
    }

    if (showEndDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = endsAtMs)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endsAtMs = dpState.selectedDateMillis
                    showEndDatePicker = false
                    showEndTimePicker = true
                }) { Text("Suivant") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Annuler") } }
        ) { DatePicker(state = dpState) }
    }

    if (showEndTimePicker) {
        var tempHour by remember { mutableStateOf(endsHour) }
        var tempMin  by remember { mutableStateOf(endsMin) }
        Dialog(onDismissRequest = { showEndTimePicker = false }) {
            Column(
                modifier = Modifier.background(Color.White, RoundedCornerShape(16.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Heure de fin", fontWeight = FontWeight.Bold, color = AdminPurple900, modifier = Modifier.padding(bottom = 16.dp))
                WheelTimePicker(
                    initialHour = endsHour,
                    initialMinute = endsMin,
                    onTimeSelected = { h, m -> tempHour = h; tempMin = m }
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showEndTimePicker = false }) { Text("Annuler") }
                    TextButton(onClick = {
                        endsHour = tempHour; endsMin = tempMin
                        showEndTimePicker = false
                    }) { Text("OK") }
                }
            }
        }
    }

    // ── Erreur ───────────────────────────────────────────────────────────────
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
                title = { Text(if (isEditMode) "Modifier la promotion" else "Nouvelle promotion", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = AdminPurple300)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminPurple900)
            )
        }
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
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nom de la promotion") },
                modifier = Modifier.fillMaxWidth()
            )

            // ── Début ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value = formatDisplay(startsAtMs, startsHour, startsMin),
                onValueChange = {},
                readOnly = true,
                label = { Text("Début *") },
                placeholder = { Text("Sélectionner une date") },
                trailingIcon = {
                    IconButton(onClick = { showStartDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AdminPurple600)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // ── Fin (obligatoire) ─────────────────────────────────────────────
            OutlinedTextField(
                value = formatDisplay(endsAtMs, endsHour, endsMin),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fin *") },
                placeholder = { Text("Sélectionner une date") },
                trailingIcon = {
                    IconButton(onClick = { showEndDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AdminPurple600)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = priceStr, onValueChange = { priceStr = it },
                label = { Text("Prix en centimes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = bonusStr, onValueChange = { bonusStr = it },
                label = { Text("Bonus : 1 gratuit tous les N (optionnel, min 2)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // ── Simulateur de rentabilité ─────────────────────────────────────
            if (bonusStr.isNotEmpty() && priceStr.isNotEmpty()) {
                SimulationPanel(
                    simulation = simulation,
                    earningsRatio = earningsRatio,
                    config = uiState.config
                )
            }

            Spacer(Modifier.height(8.dp))

            val canSubmit = !uiState.isSaving
                    && name.isNotBlank()
                    && startsAtMs != null
                    && endsAtMs != null
                    && priceStr.isNotBlank()

            Button(
                onClick = {
                    val price = priceStr.toLongOrNull() ?: return@Button
                    val bonus = bonusStr.toIntOrNull()
                    val startIso = toIso(startsAtMs!!, startsHour, startsMin)
                    val endIso = toIso(endsAtMs!!, endsHour, endsMin)
                    if (isEditMode) {
                        viewModel.update(promotionId!!, name, startIso, endIso, price, bonus)
                    } else {
                        viewModel.create(name, startIso, endIso, price, bonus)
                    }
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AdminPurple600)
            ) {
                if (uiState.isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.padding(4.dp))
                else Text(if (isEditMode) "Enregistrer" else "Créer", color = Color.White)
            }
        }
    }
}

@Composable
private fun SimulationPanel(
    simulation: Triple<Boolean, Double, Int>?,
    earningsRatio: Double,
    config: fr.uge.android.forkeat.admin.data.dto.SuperLikeConfigDTO?
) {
    val panelColor = when {
        simulation == null -> Color(0xFFF3F4F6)
        simulation.first  -> Color(0xFFDCFCE7)
        else              -> Color(0xFFFEE2E2)
    }
    val textColor = when {
        simulation == null -> Color.Gray
        simulation.first  -> Color(0xFF16A34A)
        else              -> Color(0xFFDC2626)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(panelColor, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Simulation de rentabilité", fontWeight = FontWeight.Bold, color = AdminPurple900, fontSize = 14.sp)
        Text("Ratio redistribution : ${String.format("%.0f", earningsRatio * 100)}%", color = Color.Gray, fontSize = 12.sp)
        if (config != null && simulation != null) {
            Text("N minimum pour être rentable : ${simulation.third}", color = Color.Gray, fontSize = 12.sp)
        }

        if (simulation != null) {
            Spacer(Modifier.height(4.dp))
            val (profitable, netProfit, _) = simulation
            Text(
                text = if (profitable) "✓ Rentable — bénéfice net : ${String.format("+%.2f€", netProfit)} / cycle"
                       else "✗ Non rentable — perte : ${String.format("%.2f€", netProfit)} / cycle",
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = if (profitable) "La promotion sera validée par le backend."
                       else "Le backend rejettera cette configuration.",
                color = textColor.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}
