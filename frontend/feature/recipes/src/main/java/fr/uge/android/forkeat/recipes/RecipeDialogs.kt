package fr.uge.android.forkeat.recipes

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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