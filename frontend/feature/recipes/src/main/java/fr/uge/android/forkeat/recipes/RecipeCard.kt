package fr.uge.android.forkeat.recipes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import fr.uge.android.forkeat.designsystem.theme.Gray100
import fr.uge.android.forkeat.designsystem.theme.Gray500
import fr.uge.android.forkeat.designsystem.theme.Primary500
import fr.uge.android.forkeat.designsystem.theme.Secondary700
import fr.uge.android.forkeat.designsystem.theme.Typography
import fr.uge.android.forkeat.recipes.data.dto.RecipeDTO

// Fonction utilitaire pour convertir un timestamp en une chaîne de temps relatif
fun Long.toRelativeTime(): String {
  val now = System.currentTimeMillis()
  val diff = now - this

  return when {
    diff < 1 * 60 * 1000 -> "À l'instant"
  diff < 60 * 60 * 1000 -> "Il y a ${diff / 60000} minute(s)"
    diff < 24 * 60 * 60 * 1000 -> "Il y a ${diff / 3600000} heure(s)"
    diff < 30L * 24 * 60 * 60 * 1000 -> "Il y a ${diff / 86400000} jour(s)"
    diff < 12L * 30 * 24 * 60 * 60 * 1000 -> "Il y a ${diff / 2592000000L} mois"
    else -> "Il y a ${diff / 31536000000L} an(s)"
  }
}

@Composable
fun RecipeCard(recipe: RecipeDTO, onClick: () -> Unit = {}) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .clickable {
        onClick()
      },
    shape = RoundedCornerShape(24.dp),
    colors = cardColors(containerColor = Color.White),
    elevation = cardElevation(defaultElevation = 1.dp),
    border = BorderStroke(1.dp, Gray100)
  ) {
    val timeLabel = remember(recipe.createdAt) {
      recipe.createdAt.toEpochMilliseconds().toRelativeTime()
    }

    Row(modifier = Modifier.padding(8.dp)) {
      // Image
      Image(
        painter = rememberAsyncImagePainter(recipe.imageUrl),
        contentDescription = null,
        modifier = Modifier
          .size(80.dp)
          .aspectRatio(1f)
      )
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        // Titre + infos
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = SpaceBetween
        ) {
          Text(
            recipe.title,
            style = Typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Secondary700
          )
        }
        // Description
        Text(
          recipe.summary,
          style = Typography.bodyMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          color = Gray500
        )
        Spacer(Modifier.height(4.dp))
        Text(
          "Créée $timeLabel",
          style = Typography.labelSmall,
          color = Gray500
        )
        // Auteur
        Text(
          "Par ${recipe.username}",
          style = Typography.labelSmall,
          color = Secondary700
        )
        // Temps de préparation en bas à droite
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          Text(
            "${recipe.preparationMinutes} min",
            style = Typography.labelMedium,
            color = Primary500
          )
        }
      }
    }
  }
}