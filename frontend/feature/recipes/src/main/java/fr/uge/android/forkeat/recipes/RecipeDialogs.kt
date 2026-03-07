package fr.uge.android.forkeat.recipes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.uge.android.forkeat.designsystem.theme.*

@Composable
fun SuperLikeConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Super Like cette recette ?", fontWeight = FontWeight.Bold, color = Secondary900) },
        text = { Text("Vous allez mettre en avant cette recette avec un Super Like.", color = Gray500) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Primary500),
                shape = RoundedCornerShape(50)
            ) { Text("Confirmer", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = Gray500) }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

@Composable
fun InsufficientFundsDialog(
    onDismiss: () -> Unit,
    onNavigateToWallet: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solde insuffisant", fontWeight = FontWeight.Bold, color = Secondary900) },
        text = { Text("Vous n'avez pas assez de fonds pour effectuer un Super Like. Rechargez votre wallet pour continuer.", color = Gray500) },
        confirmButton = {
            Button(
                onClick = { onDismiss(); onNavigateToWallet() },
                colors = ButtonDefaults.buttonColors(containerColor = Orange500),
                shape = RoundedCornerShape(50)
            ) { Text("Recharger", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = Gray500) }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

private val reportTypeLabels = linkedMapOf(
    "DANGEROUS" to "Contenu dangereux",
    "INAPPROPRIATE" to "Contenu inapproprié",
    "ALLERGENS" to "Erreur d'allergènes",
    "COPYRIGHT" to "Violation de copyright",
    "SPAM" to "Spam",
    "OTHER" to "Autre"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportRecipeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var step by remember { mutableIntStateOf(1) }
    var selectedType by remember { mutableStateOf("") }
    var justification by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val errorRed = Color(0xFFEF4444)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text(
                if (step == 1) "Signaler cette recette" else "Motif du signalement",
                fontWeight = FontWeight.Bold,
                color = Secondary900
            )
        },
        text = {
            if (step == 1) {
                Text(
                    "Les signalements sont examinés par notre équipe de modération. " +
                    "Merci de n'utiliser cette fonctionnalité que pour des recettes présentant " +
                    "réellement un problème.\n\n" +
                    "Un signalement abusif ou de mauvaise foi peut entraîner des sanctions sur votre compte.",
                    color = Gray500
                )
            } else {
                Column {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = reportTypeLabels[selectedType] ?: "Choisir un motif",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            reportTypeLabels.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedType = key
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = justification,
                        onValueChange = { justification = it },
                        placeholder = { Text("Expliquez brièvement la raison…", color = Gray500) },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                Button(
                    onClick = { step = 2 },
                    colors = ButtonDefaults.buttonColors(containerColor = errorRed),
                    shape = RoundedCornerShape(50)
                ) { Text("Je comprends, continuer", fontWeight = FontWeight.Bold) }
            } else {
                Button(
                    onClick = { onConfirm(selectedType, justification) },
                    enabled = selectedType.isNotEmpty() && justification.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = errorRed),
                    shape = RoundedCornerShape(50)
                ) { Text("Envoyer le signalement", fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = {
            if (step == 1) {
                TextButton(onClick = onDismiss) { Text("Annuler", color = Gray500) }
            } else {
                TextButton(onClick = { step = 1 }) { Text("Retour", color = Gray500) }
            }
        }
    )
}

private val userReportTypeLabels = linkedMapOf(
    "SPAM" to "Spam",
    "HARASSMENT" to "Harcèlement",
    "INAPPROPRIATE_CONTENT" to "Contenu inapproprié",
    "FRAUD" to "Fraude",
    "OTHER" to "Autre"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var step by remember { mutableIntStateOf(1) }
    var selectedType by remember { mutableStateOf("") }
    var justification by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val errorRed = Color(0xFFEF4444)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text(
                if (step == 1) "Signaler cet utilisateur" else "Motif du signalement",
                fontWeight = FontWeight.Bold,
                color = Secondary900
            )
        },
        text = {
            if (step == 1) {
                Text(
                    "Les signalements sont examinés par notre équipe de modération. " +
                    "Merci de n'utiliser cette fonctionnalité que pour des utilisateurs présentant " +
                    "réellement un problème.\n\n" +
                    "Un signalement abusif ou de mauvaise foi peut entraîner des sanctions sur votre compte.",
                    color = Gray500
                )
            } else {
                Column {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = userReportTypeLabels[selectedType] ?: "Choisir un motif",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            userReportTypeLabels.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedType = key
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = justification,
                        onValueChange = { justification = it },
                        placeholder = { Text("Expliquez brièvement la raison…", color = Gray500) },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                Button(
                    onClick = { step = 2 },
                    colors = ButtonDefaults.buttonColors(containerColor = errorRed),
                    shape = RoundedCornerShape(50)
                ) { Text("Je comprends, continuer", fontWeight = FontWeight.Bold) }
            } else {
                Button(
                    onClick = { onConfirm(selectedType, justification) },
                    enabled = selectedType.isNotEmpty() && justification.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = errorRed),
                    shape = RoundedCornerShape(50)
                ) { Text("Envoyer le signalement", fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = {
            if (step == 1) {
                TextButton(onClick = onDismiss) { Text("Annuler", color = Gray500) }
            } else {
                TextButton(onClick = { step = 1 }) { Text("Retour", color = Gray500) }
            }
        }
    )
}

@Composable
fun EmailNotVerifiedDialog(
    onDismiss: () -> Unit,
    onNavigateToAccount: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Email non vérifié", fontWeight = FontWeight.Bold, color = Secondary900) },
        text = { Text("Votre adresse email n'est pas encore vérifiée. Rendez-vous sur votre compte pour renvoyer le lien de confirmation.", color = Gray500) },
        confirmButton = {
            Button(
                onClick = { onDismiss(); onNavigateToAccount() },
                colors = ButtonDefaults.buttonColors(containerColor = Primary500),
                shape = RoundedCornerShape(50)
            ) { Text("Mon compte", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fermer", color = Gray500) }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}