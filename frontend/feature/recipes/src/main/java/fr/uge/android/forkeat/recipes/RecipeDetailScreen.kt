package fr.uge.android.forkeat.recipes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.tooling.preview.Preview
import fr.uge.android.forkeat.recipes.data.dto.AllergenDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeIngredientDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeStepDTO
import java.util.UUID
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.uge.android.forkeat.recipes.data.dto.RecipeDetailsDTO

@Composable
fun RecipeDetailScreen(recipe: RecipeDetailsDTO, onBack: () -> Unit) {
    System.out.println(recipe);
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
            LikeButton(recipe.nbLike.toInt(), recipe.hasLiked, recipe.id, Modifier)
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
            if (!recipe.allergens.isNullOrEmpty()) {
                Text("Allergènes", style = Typography.titleMedium, color = Color(0xFFFFC107))
                Spacer(Modifier.height(4.dp))
                AllergenBadges(recipe.allergens ?: emptyList())
                Spacer(Modifier.height(8.dp))
            }
            // Ingrédients
            if (!recipe.ingredients.isNullOrEmpty()) {
                Text("Ingrédients", style = Typography.titleMedium, color = Primary500)
                Spacer(Modifier.height(4.dp))
                Column {
                    recipe.ingredients.forEach { ingredient ->
                        IngredientCard(ingredient)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            // Préparation
            if (!recipe.steps.isNullOrEmpty()) {
                Text("Préparation", style = Typography.titleMedium, color = Secondary700)
                Spacer(Modifier.height(4.dp))
                Column {
                    recipe.steps.forEach { step ->
                        StepCard(step)
                    }
                }
            }
        }
    }
}
@Composable
fun LikeButton(
    initialCount: Int = 128,
    isLike: Boolean = false,
    recipeId: UUID,
    modifier: Modifier = Modifier,

) {
    val recipesViewModel: RecipesViewModel = viewModel()


    var isLiked by remember { mutableStateOf(isLike) }
    var count by remember { mutableIntStateOf(initialCount) }

    // Synchronise l'état local quand le paramètre change
    LaunchedEffect(isLike) {
        isLiked = isLike
    }

    LaunchedEffect(initialCount) {
        count = initialCount
    }
    
    // Animation du scale au clic
    val scale = remember { Animatable(1f) }

    // Couleur du cœur animée
    val heartColor by animateColorAsState(
        targetValue = if (isLiked) Color(0xFFFF4D6D) else Color(0xFF9E9E9E),
        animationSpec = tween(durationMillis = 300),
        label = "heartColor"
    )

    // Couleur du contour du bouton
    val borderColor by animateColorAsState(
        targetValue = if (isLiked) Color(0xFFFF4D6D) else Color(0xFFE0E0E0),
        animationSpec = tween(durationMillis = 300),
        label = "borderColor"
    )

    // Couleur de fond du bouton
    val backgroundColor by animateColorAsState(
        targetValue = if (isLiked) Color(0xFFFF4D6D).copy(alpha = 0.1f) else Color.White,
        animationSpec = tween(durationMillis = 300),
        label = "backgroundColor"
    )

    Surface(
        onClick = {
            isLiked = !isLiked
            count = if (isLiked) count + 1 else count - 1
            if(isLiked){
                recipesViewModel.likeRecipe( recipeId)
            }else{
                recipesViewModel.unlikeRecipe( recipeId)
            }
        },
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shadowElevation = if (isLiked) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cœur avec animation de scale
            val heartScale by animateFloatAsState(
                targetValue = if (isLiked) 1f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "heartScale"
            )

            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isLiked) "Unlike" else "Like",
                tint = heartColor,
                modifier = Modifier
                    .size(22.dp)
                    .scale(heartScale)
            )

            // Compteur
            Text(
                text = formatCount(count),
                color = if (isLiked) Color(0xFFFF4D6D) else Color(0xFF616161),
                fontSize = 14.sp,
                fontWeight = if (isLiked) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format("%.1fk", count / 1_000f)
        else -> count.toString()
    }
}

@Composable
fun AllergenBadges(allergens: List<AllergenDTO>) {
    Row(Modifier.padding(vertical = 4.dp)) {
        allergens.forEach { allergen ->
            Surface(
                color = Color(0xFFFFF3CD),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFFC107)),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = allergen.name.lowercase(),
                    color = Color(0xFFFFA000),
                    style = Typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun IngredientCard(ingredient: RecipeIngredientDTO) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${ingredient.quantity} ${ingredient.unit}",
                color = Primary500,
                style = Typography.labelMedium,
                modifier = Modifier.width(80.dp)
            )
            Text(
                text = ingredient.name,
                style = Typography.bodyMedium,
                color = Secondary700
            )
        }
    }
}

@Composable
fun StepCard(step: RecipeStepDTO) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(28.dp)
            ) {
                Surface(
                    color = Primary500,
                    shape = CircleShape,
                    modifier = Modifier.matchParentSize()
                ) {}
                Text(
                    text = step.stepNumber.toString(),
                    color = Color.White,
                    style = Typography.labelMedium,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = step.instruction,
                style = Typography.bodyMedium,
                color = Secondary700
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRecipeDetailScreen() {
    val exampleRecipe = RecipeDetailsDTO(
        id = UUID.randomUUID(),
        title = "Tarte aux pommes maison croustillante",
        summary = "Une tarte aux pommes délicieusement croustillante, parfaite pour les goûters d'automne. Cette recette familiale se transmet de génération en génération et séduit par sa pâte sablée, ses pommes fondantes et sa touche de cannelle. Idéale pour accompagner un thé ou un café, elle ravira petits et grands gourmands. Préparez-la à l'avance pour profiter de ses arômes envoûtants et de sa texture irrésistible. À servir tiède avec une boule de glace vanille pour un dessert encore plus gourmand !",
        parent = null,
        username = "mamie_jeanne",
        preparationMinutes = 75,
        imageUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836",
        status = "PUBLISHED",
        steps = listOf(
            RecipeStepDTO(1, "Préparez la pâte : mélangez la farine, le sel et le sucre glace. Ajoutez le beurre coupé en dés et sablez du bout des doigts."),
            RecipeStepDTO(2, "Ajoutez l'œuf, formez une boule, filmez et réservez 30 min au frais."),
            RecipeStepDTO(3, "Préchauffez le four à 180°C (th.6). Étalez la pâte et foncez un moule à tarte."),
            RecipeStepDTO(4, "Pelez, épépinez et coupez les pommes en fines lamelles. Disposez-les harmonieusement sur la pâte."),
            RecipeStepDTO(5, "Saupoudrez de cassonade et de cannelle. Ajoutez les amandes effilées."),
            RecipeStepDTO(6, "Enfournez 40-45 min jusqu'à ce que la tarte soit bien dorée."),
            RecipeStepDTO(7, "Laissez tiédir avant de déguster, éventuellement avec une boule de glace vanille.")
        ),
        ingredients = listOf(
            RecipeIngredientDTO("Farine de blé", 250.0, "g"),
            RecipeIngredientDTO("Beurre doux froid", 125.0, "g"),
            RecipeIngredientDTO("Sel", 1.0, "pincée"),
            RecipeIngredientDTO("Œuf", 1.0, "pièce"),
            RecipeIngredientDTO("Sucre glace", 50.0, "g"),
            RecipeIngredientDTO("Pommes Golden", 5.0, "pièces"),
            RecipeIngredientDTO("Cassonade", 2.0, "c. à soupe"),
            RecipeIngredientDTO("Cannelle", 1.0, "c. à café"),
            RecipeIngredientDTO("Amandes effilées", 20.0, "g")
        ),
        allergens = listOf(
            AllergenDTO("1", "Gluten", "élevé"),
            AllergenDTO("2", "Lait", "moyen"),
            AllergenDTO("3", "Oeufs", "faible"),
            AllergenDTO("4", "Fruits à coque", "faible")
        ),
        dietaryFlags = mapOf(
            "vegetarien" to true,
            "vegan" to false,
            "sans gluten" to false
        ),
        createdAt = "2026-02-10T12:00:00Z",
        updatedAt = "2026-02-10T12:00:00Z",
        10,
        false
    )
    RecipeDetailScreen(recipe = exampleRecipe, onBack = {})
}
