package fr.uge.android.forkeat.recipes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import fr.uge.android.forkeat.designsystem.theme.*
import fr.uge.android.forkeat.recipes.data.dto.PersonalizedRecipeSummaryDTO
import java.util.UUID

@Composable
fun RecipeCard(
    recipe: PersonalizedRecipeSummaryDTO,
    onRecipeClick: (String) -> Unit,
    onUsernameClick: (String) -> Unit,
    isLoggedIn: Boolean = false,
    onNavigateToLogin: () -> Unit = {},
    onLike: (UUID) -> Unit = {},
    onUnlike: (UUID) -> Unit = {},
    onSuperLike: (UUID) -> Unit = {},
) {
    val liked = recipe.likedByCurrentUser
    val likeCount = recipe.likeCount
    val superLiked = recipe.superLikedByCurrentUser
    val superLikeCount = recipe.superLikeCount

    var showSuperLikeConfirm by remember { mutableStateOf(false) }

    if (showSuperLikeConfirm) {
        SuperLikeConfirmDialog(
            onDismiss = { showSuperLikeConfirm = false },
            onConfirm = {
                showSuperLikeConfirm = false
                onSuperLike(recipe.id)
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onRecipeClick(recipe.id.toString()) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Gray100)
    ) {
        Column {
            // Image et badge durée
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(recipe.imageUrl),
                    contentDescription = recipe.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.95f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Primary500,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${recipe.preparationMinutes} min",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Secondary900
                        )
                    }
                }
            }

            // Contenu texte
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Secondary900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ── Like ────────────────────────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                if (!isLoggedIn) {
                                    onNavigateToLogin()
                                } else {
                                    if (liked) onUnlike(recipe.id) else onLike(recipe.id)
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isLoggedIn && liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isLoggedIn && liked) "Unlike" else "Like",
                            tint = if (isLoggedIn && liked) Primary500 else Gray500,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = likeCount.toString(),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Secondary800
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    // ── Super Like ──────────────────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = !isLoggedIn || !superLiked) {
                                if (!isLoggedIn) {
                                    onNavigateToLogin()
                                } else {
                                    showSuperLikeConfirm = true
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isLoggedIn && superLiked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (isLoggedIn && superLiked) Orange500 else Gray500,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = superLikeCount.toString(),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Secondary800
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // ── Auteur ──────────────────────────────────────────────
                    Text(
                        text = "@${recipe.authorUsername}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Secondary500,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onUsernameClick(recipe.authorUsername) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}