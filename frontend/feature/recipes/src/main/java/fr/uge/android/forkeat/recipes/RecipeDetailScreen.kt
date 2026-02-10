package fr.uge.android.forkeat.recipes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import fr.uge.android.forkeat.designsystem.theme.Primary500
import fr.uge.android.forkeat.designsystem.theme.Secondary700
import fr.uge.android.forkeat.designsystem.theme.Gray500
import fr.uge.android.forkeat.designsystem.theme.Typography
import fr.uge.android.forkeat.designsystem.theme.SurfaceCream
import fr.uge.android.forkeat.recipes.data.dto.RecipeDTO

@Composable
fun RecipeDetailScreen(recipe: RecipeDTO, onBack: () -> Unit) {
    Surface(color = SurfaceCream) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Primary500)) {
                Text("Retour", color = Color.White)
            }
            Spacer(Modifier.height(16.dp))
            Image(
                painter = rememberAsyncImagePainter(recipe.imageUrl),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(220.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(recipe.title, style = Typography.titleLarge, color = Secondary700)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Par ${recipe.username}", style = Typography.labelSmall, color = Gray500)
                Spacer(Modifier.width(16.dp))
                Text("${recipe.preparationMinutes} min", style = Typography.labelMedium, color = Primary500)
            }
            Spacer(Modifier.height(16.dp))
            Text(recipe.summary, style = Typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            // Allergènes
            if (recipe.allergens.isNotEmpty()) {
                Text("Allergènes", style = Typography.titleMedium, color = Color(0xFFFFC107))
                Text(recipe.allergens.joinToString(", "), style = Typography.bodyMedium, color = Color(0xFFFFC107))
                Spacer(Modifier.height(8.dp))
            }
            // Ingrédients
            if (recipe.ingredients.isNotEmpty()) {
                Text("Ingrédients", style = Typography.titleMedium, color = Primary500)
                Column {
                    recipe.ingredients.forEach { ingredient ->
                        Text("- $ingredient", style = Typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            // Préparation
            if (recipe.steps.isNotEmpty()) {
                Text("Préparation", style = Typography.titleMedium, color = Secondary700)
                Column {
                    recipe.steps.forEachIndexed { idx, step ->
                        Text("${idx + 1}. $step", style = Typography.bodyMedium)
                    }
                }
            }
        }
    }
}
