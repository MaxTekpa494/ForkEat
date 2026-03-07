package fr.uge.android.forkeat.recipes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import fr.uge.android.forkeat.designsystem.theme.*
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.recipes.data.dto.AllergenDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeDetailsDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeDiffDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeIngredientDTO
import fr.uge.android.forkeat.recipes.data.dto.RecipeStepDTO
import java.util.UUID
import kotlin.time.Instant

@Composable
fun RecipeDetailScreen(
    recipe: RecipeDetailsDTO,
    parent: RecipeDTO? = null,
    diff: RecipeDiffDTO? = null,
    isOwner: Boolean = false,
    isAuthenticated: Boolean = false,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onCreateVariant: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToMyProfile: () -> Unit = {},
    onLike: (UUID) -> Unit = {},
    onUnlike: (UUID) -> Unit = {},
    onSuperLike: (UUID) -> Unit = {},
    onFollow: (UUID) -> Unit = {},
    onUnfollow: (UUID) -> Unit = {},
    insufficientFunds: Boolean = false,
    onDismissInsufficientFunds: () -> Unit = {},
    emailNotVerified: Boolean = false,
    onDismissEmailNotVerified: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToWallet: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onReport: (UUID, String, String) -> Unit = { id, type, justification -> },    reportSuccess: Boolean = false,
    onDismissReportSuccess: () -> Unit = {},
    reportAlreadyReported: Boolean = false,
    onDismissReportAlreadyReported: () -> Unit = {},
) {
    var diffModeActive by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSuperLikeConfirm by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    val currentUsername = remember { ForkEatApi.getCurrentUsername() }

    if (insufficientFunds) {
        InsufficientFundsDialog(
            onDismiss = onDismissInsufficientFunds,
            onNavigateToWallet = onNavigateToWallet
        )
    }

    if (emailNotVerified) {
        EmailNotVerifiedDialog(
            onDismiss = onDismissEmailNotVerified,
            onNavigateToAccount = onNavigateToAccount,
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer la recette", fontWeight = FontWeight.Bold, color = Secondary900) },
            text = { Text("Voulez-vous vraiment supprimer « ${recipe.title} » ?\nCette action est irréversible.", color = Gray500) },
            confirmButton = {
                Button(
                    onClick = { 
                        showDeleteDialog = false
                        onDelete() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(50)
                ) { Text("Supprimer", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler", color = Gray500)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    if (showSuperLikeConfirm) {
        SuperLikeConfirmDialog(
            onDismiss = { showSuperLikeConfirm = false },
            onConfirm = {
                showSuperLikeConfirm = false
                onSuperLike(recipe.id)
            }
        )
    }

    if (showReportDialog) {
        ReportRecipeDialog(
            onDismiss = { showReportDialog = false },
            onConfirm = { type, justification ->
                showReportDialog = false
                onReport(recipe.id, type, justification)
            }
        )
    }

    if (reportSuccess) {
        AlertDialog(
            onDismissRequest = onDismissReportSuccess,
            title = { Text("Signalement envoyé", fontWeight = FontWeight.Bold, color = Secondary900) },
            text = { Text("Votre signalement a bien été enregistré. Notre équipe va l'examiner.", color = Gray500) },
            confirmButton = {
                Button(
                    onClick = onDismissReportSuccess,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary500),
                    shape = RoundedCornerShape(50)
                ) { Text("OK", fontWeight = FontWeight.Bold) }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    if (reportAlreadyReported) {
        AlertDialog(
            onDismissRequest = onDismissReportAlreadyReported,
            title = { Text("Déjà signalée", fontWeight = FontWeight.Bold, color = Secondary900) },
            text = { Text("Vous avez déjà signalé cette recette.", color = Gray500) },
            confirmButton = {
                Button(
                    onClick = onDismissReportAlreadyReported,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary500),
                    shape = RoundedCornerShape(50)
                ) { Text("OK", fontWeight = FontWeight.Bold) }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    Surface(color = SurfaceCream) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Primary500),
                shape = RoundedCornerShape(50)
            ) { Text("Retour", fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(16.dp))

            if (parent != null) {
                VariantBanner(
                    parentTitle = parent.title,
                    diffModeActive = diffModeActive,
                    onToggleDiffMode = { diffModeActive = !diffModeActive }
                )
                Spacer(Modifier.height(12.dp))
            }

            if (!recipe.imageUrl.isNullOrEmpty()) {
                Box {
                    Image(
                        painter = rememberAsyncImagePainter(recipe.imageUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                    if (diff?.imageChanged == true) {
                        Surface(
                            color = Orange500,
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        ) {
                            Text(
                                "Image modifiée",
                                color = Color.White,
                                style = Typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(recipe.title, style = Typography.titleLarge, color = Secondary700)
                    if (diff?.titleChanged == true && diff.originalTitle != null) {
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            color = Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(50.dp),
                            border = BorderStroke(1.dp, Color(0xFFFCD34D))
                        ) {
                            Text(
                                text = "Titre modifié · Original : ${diff.originalTitle}",
                                style = Typography.labelSmall,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isAuthenticated) {
                        OutlinedButton(
                            onClick = onCreateVariant,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Primary500),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Variante", color = Primary500, style = Typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (isOwner) {
                        Button(
                            onClick = onEdit,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Secondary700),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Modifier", color = Color.White, style = Typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showDeleteDialog = true },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Supprimer", color = Color.White, style = Typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LikeButton(
                    initialCount = recipe.nbLike.toInt(),
                    isLike = recipe.hasLiked,
                    recipeId = recipe.id,
                    isAuthenticated = isAuthenticated,
                    onLike = onLike,
                    onUnlike = onUnlike,
                    onNavigateToLogin = onNavigateToLogin
                )
                SuperLikeButton(
                    initialCount = recipe.nbSuperLike.toInt(),
                    isSuperLiked = recipe.hasSuperLiked,
                    onSuperLikeClick = {
                        if (isAuthenticated) {
                            showSuperLikeConfirm = true
                        } else {
                            onNavigateToLogin()
                        }
                    }
                )
                FollowButton(
                    initialCount = recipe.nbFollow.toInt(),
                    isFollowed = recipe.hasFollowed,
                    recipeId = recipe.id,
                    isAuthenticated = isAuthenticated,
                    onFollow = onFollow,
                    onUnfollow = onUnfollow,
                    onNavigateToLogin = onNavigateToLogin
                )
            }
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "@${recipe.username}",
                    style = Typography.labelSmall,
                    color = Gray500,
                    modifier = Modifier.clickable {
                        if (currentUsername != null && recipe.username == currentUsername) {
                            onNavigateToMyProfile()
                        } else {
                            onNavigateToUserProfile(recipe.username)
                        }
                    }
                )
                Spacer(Modifier.width(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("${recipe.preparationMinutes} min", style = Typography.labelMedium, color = Primary500)
                    if (diff != null && diff.timeDelta != 0) {
                        val isPositive = diff.timeDelta > 0
                        Surface(
                            color = if (isPositive) Color(0xFFFFF7ED) else Color(0xFFECFDF5),
                            shape = RoundedCornerShape(50.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isPositive) Color(0xFFFBD38D) else Color(0xFF6EE7B7)
                            )
                        ) {
                            Text(
                                text = "${if (isPositive) "+" else ""}${diff.timeDelta} min vs original",
                                style = Typography.labelSmall,
                                color = if (isPositive) Color(0xFFB45309) else Color(0xFF059669),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Text(recipe.summary, style = Typography.bodyMedium)
            val originalSummary = diff?.originalSummary
            if (diff?.summaryChanged == true && originalSummary != null) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            "Description originale",
                            style = Typography.labelSmall,
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = originalSummary,
                            style = Typography.bodySmall,
                            color = Color(0xFFB45309),
                            textDecoration = TextDecoration.LineThrough,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            if (diff != null && diff.allergens.isNotEmpty()) {
                Text("Allergènes", style = Typography.titleMedium, color = Color(0xFFFFC107))
                Spacer(Modifier.height(4.dp))
                DiffAllergenBadges(diff.allergens, diffModeActive)
                Spacer(Modifier.height(8.dp))
            } else if (recipe.allergens.isNotEmpty()) {
                Text("Allergènes", style = Typography.titleMedium, color = Color(0xFFFFC107))
                Spacer(Modifier.height(4.dp))
                AllergenBadges(recipe.allergens)
                Spacer(Modifier.height(8.dp))
            }

            if (diff != null && diff.dietaryFlags.isNotEmpty()) {
                Text("Régimes alimentaires", style = Typography.titleMedium, color = Color(0xFF4CAF50))
                Spacer(Modifier.height(4.dp))
                DiffDietaryFlagBadges(diff.dietaryFlags, diffModeActive)
                Spacer(Modifier.height(8.dp))
            } else if (recipe.dietaries.isNotEmpty()) {
                Text("Régimes alimentaires", style = Typography.titleMedium, color = Color(0xFF4CAF50))
                Spacer(Modifier.height(4.dp))
                DietaryFlagBadges(recipe.dietaries)
                Spacer(Modifier.height(8.dp))
            }

            if (diff != null && diff.ingredients.isNotEmpty()) {
                Text("Ingrédients", style = Typography.titleMedium, color = Primary500)
                Spacer(Modifier.height(4.dp))
                diff.ingredients.forEach { DiffIngredientCard(it, diffModeActive) }
                Spacer(Modifier.height(8.dp))
            } else if (recipe.ingredients.isNotEmpty()) {
                Text("Ingrédients", style = Typography.titleMedium, color = Primary500)
                Spacer(Modifier.height(4.dp))
                recipe.ingredients.forEach { IngredientCard(it) }
                Spacer(Modifier.height(8.dp))
            }

            if (diff != null && diff.steps.isNotEmpty()) {
                Text("Préparation", style = Typography.titleMedium, color = Secondary700)
                Spacer(Modifier.height(4.dp))
                diff.steps.forEach { DiffStepCard(it, diffModeActive) }
            } else if (recipe.steps.isNotEmpty()) {
                Text("Préparation", style = Typography.titleMedium, color = Secondary700)
                Spacer(Modifier.height(4.dp))
                recipe.steps.forEach { StepCard(it) }
            }

            if (isAuthenticated && !isOwner) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showReportDialog = true },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "Signaler cette recette",
                            color = Gray500.copy(alpha = 0.5f),
                            style = Typography.labelSmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VariantBanner(
    parentTitle: String,
    diffModeActive: Boolean,
    onToggleDiffMode: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = Color(0xFFEEF2FF),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("⎇", color = Primary500, fontSize = 16.sp)
                    }
                }
                Column {
                    Text("Variante de", style = Typography.labelSmall, color = Gray500)
                    Text(
                        text = parentTitle,
                        style = Typography.labelMedium,
                        color = Secondary700,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (diffModeActive) "Diff" else "Complet",
                    style = Typography.labelSmall,
                    color = if (diffModeActive) Secondary700 else Gray500,
                    fontWeight = if (diffModeActive) FontWeight.SemiBold else FontWeight.Normal
                )
                Switch(
                    checked = diffModeActive,
                    onCheckedChange = { onToggleDiffMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Primary500,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFD1D5DB)
                    )
                )
            }
        }
    }
}

@Composable
private fun DiffBadge(type: RecipeDiffDTO.DiffType) {
    if (type == RecipeDiffDTO.DiffType.UNCHANGED) return
    val (label, textColor, bgColor) = when (type) {
        RecipeDiffDTO.DiffType.ADDED    -> Triple("Ajouté",   Color(0xFF166534), Color(0xFFDCFCE7))
        RecipeDiffDTO.DiffType.REMOVED  -> Triple("Supprimé", Color(0xFF9F1239), Color(0xFFFFE4E6))
        RecipeDiffDTO.DiffType.MODIFIED -> Triple("Modifié",  Color(0xFFB45309), Color(0xFFFFFBEB))
        else -> return
    }
    Surface(color = bgColor, shape = RoundedCornerShape(50.dp)) {
        Text(
            text = label,
            style = Typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun AllergenBadges(allergens: List<AllergenDTO>) {
    Row(
        Modifier
            .padding(vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        allergens.forEach { allergen ->
            Surface(
                color = Color(0xFFFFF3CD),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFFC107))
            ) {
                Text(
                    text = allergen.name.lowercase(),
                    color = Color(0xFFFFA000),
                    style = Typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DiffAllergenBadges(
    allergens: List<RecipeDiffDTO.AllergenDiffDTO>,
    diffModeActive: Boolean
) {
    Row(
        Modifier
            .padding(vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        allergens.forEach { allergen ->
            val alpha = if (diffModeActive && allergen.type == RecipeDiffDTO.DiffType.UNCHANGED) 0.15f else 1f
            val isRemoved = allergen.type == RecipeDiffDTO.DiffType.REMOVED
            val isAdded   = allergen.type == RecipeDiffDTO.DiffType.ADDED
            val bgColor = when (allergen.type) {
                RecipeDiffDTO.DiffType.ADDED    -> Color(0xFFDCFCE7)
                RecipeDiffDTO.DiffType.REMOVED  -> Color(0xFFFFE4E6)
                else                            -> Color(0xFFFFF3CD)
            }
            val textColor = when (allergen.type) {
                RecipeDiffDTO.DiffType.ADDED    -> Color(0xFF166534)
                RecipeDiffDTO.DiffType.REMOVED  -> Color(0xFF9F1239)
                else                            -> Color(0xFFFFA000)
            }
            val borderColor = when (allergen.type) {
                RecipeDiffDTO.DiffType.ADDED    -> Color(0xFF86EFAC)
                RecipeDiffDTO.DiffType.REMOVED  -> Color(0xFFFCA5A5)
                else                            -> Color(0xFFFFC107)
            }
            Surface(
                color = bgColor,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.alpha(alpha)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = allergen.name.lowercase(),
                        color = textColor,
                        style = Typography.labelMedium,
                        textDecoration = if (isRemoved) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (allergen.type != RecipeDiffDTO.DiffType.UNCHANGED) {
                        Text(
                            text = if (isAdded) "Ajouté" else "Supprimé",
                            color = textColor,
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DietaryFlagBadges(dietaries: List<String>) {
    Row(
        Modifier
            .padding(vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dietaries.forEach { name ->
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFA5D6A7))
            ) {
                Text(
                    text = name,
                    color = Color(0xFF2E7D32),
                    style = Typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DiffDietaryFlagBadges(
    flags: List<RecipeDiffDTO.DietaryFlagDiffDTO>,
    diffModeActive: Boolean
) {
    Row(
        Modifier
            .padding(vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        flags.forEach { flag ->
            val alpha = if (diffModeActive && flag.type == RecipeDiffDTO.DiffType.UNCHANGED) 0.15f else 1f
            val isRemoved = flag.type == RecipeDiffDTO.DiffType.REMOVED
            val isAdded   = flag.type == RecipeDiffDTO.DiffType.ADDED
            val bgColor = when (flag.type) {
                RecipeDiffDTO.DiffType.ADDED    -> Color(0xFFDCFCE7)
                RecipeDiffDTO.DiffType.REMOVED  -> Color(0xFFFFE4E6)
                else                            -> Color(0xFFE8F5E9)
            }
            val textColor = when (flag.type) {
                RecipeDiffDTO.DiffType.ADDED    -> Color(0xFF166534)
                RecipeDiffDTO.DiffType.REMOVED  -> Color(0xFF9F1239)
                else                            -> Color(0xFF2E7D32)
            }
            val borderColor = when (flag.type) {
                RecipeDiffDTO.DiffType.ADDED    -> Color(0xFF86EFAC)
                RecipeDiffDTO.DiffType.REMOVED  -> Color(0xFFFCA5A5)
                else                            -> Color(0xFFA5D6A7)
            }
            Surface(
                color = bgColor,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.alpha(alpha)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = flag.flagName,
                        color = textColor,
                        style = Typography.labelMedium,
                        textDecoration = if (isRemoved) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (flag.type != RecipeDiffDTO.DiffType.UNCHANGED) {
                        Text(
                            text = if (isAdded) "Ajouté" else "Retiré",
                            color = textColor,
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatQty(ingredient.quantity)} ${ingredient.unit}",
                color = Primary500,
                style = Typography.labelMedium,
                modifier = Modifier.width(80.dp)
            )
            Text(text = ingredient.name, style = Typography.bodyMedium, color = Secondary700)
        }
    }
}

@Composable
private fun DiffIngredientCard(
    ingredient: RecipeDiffDTO.IngredientDiffDTO,
    diffModeActive: Boolean
) {
    val alpha = if (diffModeActive && ingredient.type == RecipeDiffDTO.DiffType.UNCHANGED) 0.15f else 1f
    val isRemoved  = ingredient.type == RecipeDiffDTO.DiffType.REMOVED
    val isModified = ingredient.type == RecipeDiffDTO.DiffType.MODIFIED
    val isAdded    = ingredient.type == RecipeDiffDTO.DiffType.ADDED
    val bgColor = when (ingredient.type) {
        RecipeDiffDTO.DiffType.ADDED    -> Color(0xFFDCFCE7)
        RecipeDiffDTO.DiffType.REMOVED  -> Color(0xFFFFE4E6)
        RecipeDiffDTO.DiffType.MODIFIED -> Color(0xFFFFF8E1)
        RecipeDiffDTO.DiffType.UNCHANGED -> Color.White
    }
    val borderColor = when (ingredient.type) {
        RecipeDiffDTO.DiffType.ADDED    -> Color(0xFF86EFAC)
        RecipeDiffDTO.DiffType.REMOVED  -> Color(0xFFFCA5A5)
        RecipeDiffDTO.DiffType.MODIFIED -> Color(0xFFFCD34D)
        RecipeDiffDTO.DiffType.UNCHANGED -> Color(0xFFE5E7EB)
    }
    val nameColor = when {
        isRemoved  -> Color(0xFF9F1239)
        isModified -> Color(0xFFB45309)
        isAdded    -> Color(0xFF166534)
        else       -> Secondary700
    }
    val qtyColor = when {
        isRemoved  -> Color(0xFF9F1239)
        isModified -> Color(0xFFB45309)
        isAdded    -> Color(0xFF166534)
        else       -> Primary500
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.padding(bottom = 8.dp).alpha(alpha)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.width(90.dp)) {
                    if (isModified) {
                        Text(
                            text = "${formatQty(ingredient.originalQuantity)} ${ingredient.originalUnit}",
                            style = Typography.labelSmall,
                            color = Color(0xFFF59E0B),
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    Text(
                        text = "${formatQty(ingredient.quantity)} ${ingredient.unit}",
                        color = qtyColor,
                        style = Typography.labelMedium,
                        textDecoration = if (isRemoved) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
                Text(
                    text = ingredient.name,
                    style = Typography.bodyMedium,
                    color = nameColor,
                    textDecoration = if (isRemoved) TextDecoration.LineThrough else TextDecoration.None
                )
            }
            DiffBadge(ingredient.type)
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
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                Surface(color = Primary500, shape = CircleShape, modifier = Modifier.matchParentSize()) {}
                Text(text = step.stepNumber.toString(), color = Color.White, style = Typography.labelMedium)
            }
            Spacer(Modifier.width(12.dp))
            Text(text = step.instruction, style = Typography.bodyMedium, color = Secondary700)
        }
    }
}

@Composable
private fun DiffStepCard(
    step: RecipeDiffDTO.StepDiffDTO,
    diffModeActive: Boolean
) {
    val alpha = if (diffModeActive && step.type == RecipeDiffDTO.DiffType.UNCHANGED) 0.15f else 1f
    val isRemoved  = step.type == RecipeDiffDTO.DiffType.REMOVED
    val isModified = step.type == RecipeDiffDTO.DiffType.MODIFIED
    val isAdded    = step.type == RecipeDiffDTO.DiffType.ADDED
    val bgColor = when (step.type) {
        RecipeDiffDTO.DiffType.ADDED    -> Color(0xFFDCFCE7)
        RecipeDiffDTO.DiffType.REMOVED  -> Color(0xFFFFE4E6)
        RecipeDiffDTO.DiffType.MODIFIED -> Color(0xFFFFF8E1)
        RecipeDiffDTO.DiffType.UNCHANGED -> Color(0xFFF8FAFC)
    }
    val borderColor = when (step.type) {
        RecipeDiffDTO.DiffType.ADDED    -> Color(0xFF86EFAC)
        RecipeDiffDTO.DiffType.REMOVED  -> Color(0xFFFCA5A5)
        RecipeDiffDTO.DiffType.MODIFIED -> Color(0xFFFCD34D)
        RecipeDiffDTO.DiffType.UNCHANGED -> Color(0xFFE5E7EB)
    }
    val numberColor = when (step.type) {
        RecipeDiffDTO.DiffType.ADDED    -> Color(0xFF22C55E)
        RecipeDiffDTO.DiffType.REMOVED  -> Color(0xFFF87171)
        RecipeDiffDTO.DiffType.MODIFIED -> Color(0xFFF59E0B)
        RecipeDiffDTO.DiffType.UNCHANGED -> Color(0xFF9CA3AF)
    }
    val textColor = when {
        isRemoved  -> Color(0xFF9F1239)
        isModified -> Color(0xFFB45309)
        isAdded    -> Color(0xFF166534)
        else       -> Secondary700
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.padding(bottom = 8.dp).alpha(alpha)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                Surface(color = numberColor, shape = CircleShape, modifier = Modifier.matchParentSize()) {}
                Text(text = step.stepNumber.toString(), color = Color.White, style = Typography.labelMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = step.instruction,
                        style = Typography.bodyMedium,
                        color = textColor,
                        textDecoration = if (isRemoved) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    DiffBadge(step.type)
                }
                val originalInstruction = step.originalInstruction
                if (isModified && !originalInstruction.isNullOrEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = Color(0xFFFCD34D), thickness = 1.dp)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Original",
                            style = Typography.labelSmall,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = originalInstruction,
                            style = Typography.bodySmall,
                            color = Color(0xFFF59E0B),
                            textDecoration = TextDecoration.LineThrough,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LikeButton(
    initialCount: Int = 0,
    isLike: Boolean = false,
    recipeId: UUID,
    modifier: Modifier = Modifier,
    isAuthenticated: Boolean = false,
    onLike: (UUID) -> Unit = {},
    onUnlike: (UUID) -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    var isLiked by remember { mutableStateOf(isLike) }
    var count by remember { mutableIntStateOf(initialCount) }

    LaunchedEffect(isLike) { isLiked = isLike }
    LaunchedEffect(initialCount) { count = initialCount }

    val heartColor by animateColorAsState(
        targetValue = if (isLiked) Primary500 else Color(0xFF9E9E9E),
        animationSpec = tween(durationMillis = 300),
        label = "heartColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isLiked) Primary500 else Color(0xFFE0E0E0),
        animationSpec = tween(durationMillis = 300),
        label = "borderColor"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isLiked) Primary500.copy(alpha = 0.1f) else Color.White,
        animationSpec = tween(durationMillis = 300),
        label = "backgroundColor"
    )

    Surface(
        onClick = {
            if (!isAuthenticated) {
                onNavigateToLogin()
                return@Surface
            }
            val targetLiked = !isLiked
            isLiked = targetLiked
            count = if (targetLiked) count + 1 else count - 1
            if (targetLiked) onLike(recipeId)
            else onUnlike(recipeId)
        },
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isLiked) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val heartScale by animateFloatAsState(
                targetValue = if (isLiked) 1.2f else 1f,
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
                modifier = Modifier.size(22.dp).scale(heartScale)
            )
            Text(
                text = formatCount(count),
                color = if (isLiked) Primary500 else Color(0xFF616161),
                fontSize = 14.sp,
                fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun SuperLikeButton(
    initialCount: Int = 0,
    isSuperLiked: Boolean = false,
    onSuperLikeClick: () -> Unit = {}
) {
    val starColor by animateColorAsState(
        targetValue = if (isSuperLiked) Orange500 else Color(0xFF9E9E9E),
        animationSpec = tween(durationMillis = 300),
        label = "starColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSuperLiked) Orange500 else Color(0xFFE0E0E0),
        animationSpec = tween(durationMillis = 300),
        label = "borderColor"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSuperLiked) Orange500.copy(alpha = 0.1f) else Color.White,
        animationSpec = tween(durationMillis = 300),
        label = "backgroundColor"
    )

    Surface(
        onClick = { if (!isSuperLiked) onSuperLikeClick() },
        modifier = Modifier,
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isSuperLiked) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isSuperLiked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (isSuperLiked) "Super liké" else "Super like",
                tint = starColor,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = formatCount(initialCount),
                color = if (isSuperLiked) Orange500 else Color(0xFF616161),
                fontSize = 14.sp,
                fontWeight = if (isSuperLiked) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun FollowButton(
    initialCount: Int = 0,
    isFollowed: Boolean = false,
    recipeId: UUID,
    modifier: Modifier = Modifier,
    isAuthenticated: Boolean = false,
    onFollow: (UUID) -> Unit = {},
    onUnfollow: (UUID) -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    var isFollowing by remember { mutableStateOf(isFollowed) }
    var count by remember { mutableIntStateOf(initialCount) }

    LaunchedEffect(isFollowed) { isFollowing = isFollowed }
    LaunchedEffect(initialCount) { count = initialCount }

    val bookmarkColor by animateColorAsState(
        targetValue = if (isFollowing) Primary500 else Color(0xFF9E9E9E),
        animationSpec = tween(durationMillis = 300),
        label = "bookmarkColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFollowing) Primary500 else Color(0xFFE0E0E0),
        animationSpec = tween(durationMillis = 300),
        label = "borderColor"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isFollowing) Primary500.copy(alpha = 0.1f) else Color.White,
        animationSpec = tween(durationMillis = 300),
        label = "backgroundColor"
    )

    Surface(
        onClick = {
            if (!isAuthenticated) {
                onNavigateToLogin()
                return@Surface
            }
            val targetFollowed = !isFollowing
            isFollowing = targetFollowed
            count = if (targetFollowed) count + 1 else count - 1
            if (targetFollowed) onFollow(recipeId)
            else onUnfollow(recipeId)
        },
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isFollowing) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isFollowing) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (isFollowing) "Ne plus suivre" else "Suivre",
                tint = bookmarkColor,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = formatCount(count),
                color = if (isFollowing) Primary500 else Color(0xFF616161),
                fontSize = 14.sp,
                fontWeight = if (isFollowing) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

private fun formatQty(qty: Double): String =
    if (qty == qty.toLong().toDouble()) qty.toLong().toString() else qty.toString()

fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
    count >= 1_000     -> String.format("%.1fk", count / 1_000f)
    else               -> count.toString()
}

@Preview(showBackground = true)
@Composable
fun PreviewRecipeDetailScreen() {
    val exampleRecipe = RecipeDetailsDTO(
        id = UUID.randomUUID(),
        title = "Tarte aux pommes maison croustillante",
        summary = "Une tarte aux pommes délicieusement croustillante.",
        parentId = null,
        username = "mamie_jeanne",
        preparationMinutes = 75,
        imageUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836",
        status = "PUBLISHED",
        steps = listOf(
            RecipeStepDTO(1, "Préparez la pâte : mélangez la farine, le sel et le sucre glace."),
            RecipeStepDTO(2, "Ajoutez l'œuf, formez une boule et réservez 30 min au frais.")
        ),
        ingredients = listOf(
            RecipeIngredientDTO("Farine de blé", 250.0, "g"),
            RecipeIngredientDTO("Beurre doux froid", 125.0, "g")
        ),
        allergens = listOf(
            AllergenDTO("1", "Gluten", "élevé"),
            AllergenDTO("2", "Lait", "moyen")
        ),
        dietaries = listOf("vegetarian"),
        createdAt = Instant.parse("2026-02-10T12:00:00Z"),
        updatedAt = Instant.parse("2026-02-10T12:00:00Z"),
        nbLike = 10,
        hasLiked = false,
        nbSuperLike = 2,
        hasSuperLiked = false
    )
    RecipeDetailScreen(recipe = exampleRecipe, onBack = {})
}

@Preview(showBackground = true, name = "Variante avec diff")
@Composable
fun PreviewRecipeDetailScreenWithDiff() {
    val parentRecipe = RecipeDTO(
        id = UUID.randomUUID(),
        title = "Tarte aux pommes classique",
        summary = "Recette originale",
        parentId = null,
        username = "mamie_jeanne",
        preparationMinutes = 60,
        imageUrl = "",
        status = "PUBLISHED",
        steps = emptyList(),
        ingredients = emptyList(),
        allergens = emptyList(),
        dietaries = emptyList(),
        createdAt = Instant.parse("2026-02-10T12:00:00Z"),
        updatedAt = Instant.parse("2026-02-10T12:00:00Z"),
        nbLike = 5,
        hasLiked = false
    )
    val diff = RecipeDiffDTO(
        titleChanged = true,
        originalTitle = "Tarte aux pommes classique",
        originalSummary = "Recette originale",
        summaryChanged = false,
        imageChanged = false,
        timeDelta = 15,
        ingredients = listOf(
            RecipeDiffDTO.IngredientDiffDTO(RecipeDiffDTO.DiffType.UNCHANGED, "Farine", 250.0, "g", 250.0, "g"),
            RecipeDiffDTO.IngredientDiffDTO(RecipeDiffDTO.DiffType.ADDED, "Cannelle", 1.0, "c. à café", 0.0, ""),
            RecipeDiffDTO.IngredientDiffDTO(RecipeDiffDTO.DiffType.REMOVED, "Vanille", 0.0, "", 1.0, "gousse"),
            RecipeDiffDTO.IngredientDiffDTO(RecipeDiffDTO.DiffType.MODIFIED, "Beurre", 150.0, "g", 125.0, "g")
        ),
        steps = listOf(
            RecipeDiffDTO.StepDiffDTO(RecipeDiffDTO.DiffType.UNCHANGED, 1, "Préparez la pâte."),
            RecipeDiffDTO.StepDiffDTO(RecipeDiffDTO.DiffType.MODIFIED, 2, "Ajoutez la cannelle.", "Ajoutez la vanille."),
            RecipeDiffDTO.StepDiffDTO(RecipeDiffDTO.DiffType.ADDED, 3, "Saupoudrez de cassonade.")
        ),
        allergens = listOf(
            RecipeDiffDTO.AllergenDiffDTO(RecipeDiffDTO.DiffType.UNCHANGED, "Gluten"),
            RecipeDiffDTO.AllergenDiffDTO(RecipeDiffDTO.DiffType.ADDED, "Noix")
        ),
        dietaryFlags = emptyList()
    )
    val recipe = RecipeDetailsDTO(
        id = UUID.randomUUID(),
        title = "Tarte aux pommes maison croustillante",
        summary = "Ma version améliorée de la tarte.",
        parentId = parentRecipe.id,
        username = "chef_alex",
        preparationMinutes = 75,
        imageUrl = "",
        status = "PUBLISHED",
        steps = listOf(RecipeStepDTO(1, "Préparez la pâte."), RecipeStepDTO(2, "Ajoutez la cannelle.")),
        ingredients = listOf(RecipeIngredientDTO("Farine", 250.0, "g")),
        allergens = listOf(AllergenDTO("1", "Gluten", "élevé")),
        dietaries = listOf("vegetarian"),
        createdAt = Instant.parse("2026-02-10T12:00:00Z"),
        updatedAt = Instant.parse("2026-02-10T12:00:00Z"),
        nbLike = 3,
        hasLiked = false,
        nbSuperLike = 0,
        hasSuperLiked = false
    )
    RecipeDetailScreen(
        recipe = recipe,
        parent = parentRecipe,
        diff = diff,
        isOwner = true,
        isAuthenticated = true,
        onBack = {}
    )
}
