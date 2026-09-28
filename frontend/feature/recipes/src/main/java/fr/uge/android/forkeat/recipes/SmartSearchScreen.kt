package fr.uge.android.forkeat.recipes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.uge.android.forkeat.designsystem.theme.*
import fr.uge.android.forkeat.recipes.data.dto.PersonalizedRecipeSummaryDTO
import java.util.UUID

@Composable
fun SmartSearchScreen(
    query: String,
    results: List<PersonalizedRecipeSummaryDTO>,
    isLoading: Boolean,
    hasSearched: Boolean,
    error: String?,
    isModerationError: Boolean,
    balance: Long?,
    smartSearchCost: Long?,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onNewSearch: () -> Unit,
    onBack: () -> Unit,
    onRecipeClick: (String) -> Unit,
    onNavigateToWallet: () -> Unit,
    isLoggedIn: Boolean = false,
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToMyProfile: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onLike: (UUID) -> Unit = {},
    onUnlike: (UUID) -> Unit = {},
    onSuperLike: (UUID) -> Unit = {},
    onFollow: (UUID) -> Unit = {},
    onUnfollow: (UUID) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize().background(SurfaceCream)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {

                // ── Section hero + champ de recherche ────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Secondary800, Secondary900)
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 28.dp)
                    ) {
                        Column {
                            // Badge IA
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Primary500.copy(alpha = 0.2f),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Text(
                                    "Propulsé par l'IA",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    color = Primary300,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                "Décrivez votre envie culinaire",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Secondary300,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Carte de saisie
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    OutlinedTextField(
                                        value = query,
                                        onValueChange = onQueryChange,
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = {
                                            Text(
                                                "Ex : un plat végétarien rapide sans gluten...",
                                                color = Color.White.copy(alpha = 0.4f)
                                            )
                                        },
                                        minLines = 3,
                                        maxLines = 5,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            cursorColor = Primary500,
                                            disabledTextColor = Color.White.copy(alpha = 0.5f),
                                            disabledBorderColor = Color.White.copy(alpha = 0.1f),
                                            disabledContainerColor = Color.Transparent,
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isLoading,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                onSearch()
                                                focusManager.clearFocus()
                                            }
                                        )
                                    )

                                    Spacer(Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Solde & coût
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            if (balance != null) {
                                                Text(
                                                    "Solde : ${"%.2f".format(balance / 100.0)} €",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.75f)
                                                )
                                            }
                                            Text(
                                                if (smartSearchCost != null) "Coût : ${"%.2f".format(smartSearchCost / 100.0)} €" else "Coût : …",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        }

                                        // Bouton recherche
                                        Button(
                                            onClick = {
                                                onSearch()
                                                focusManager.clearFocus()
                                            },
                                            enabled = query.isNotBlank() && !isLoading,
                                            shape = RoundedCornerShape(50),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Primary500,
                                                disabledContainerColor = Primary500.copy(alpha = 0.35f)
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("Rechercher", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Contenu conditionnel ──────────────────────────────────────
                when {
                    !hasSearched -> {
                        item { HowItWorksSection(smartSearchCost) }
                    }

                    isModerationError -> {
                        item { ModerationErrorSection(onNewSearch) }
                    }

                    error != null -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                if (error.contains("insuffisant", ignoreCase = true)) {
                                    Button(
                                        onClick = onNavigateToWallet,
                                        colors = ButtonDefaults.buttonColors(containerColor = Orange500),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text("Recharger mon wallet", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    results.isEmpty() -> {
                        item { NoResultsSection(onNewSearch) }
                    }

                    else -> {
                        // En-tête résultats
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 16.dp, bottom = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        buildString {
                                            append(results.size)
                                            append(" recette")
                                            if (results.size > 1) append("s")
                                            append(" trouvée")
                                            if (results.size > 1) append("s")
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Secondary900
                                    )
                                    TextButton(onClick = onNewSearch) {
                                        Text(
                                            "Nouvelle recherche",
                                            color = Primary500,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                Text(
                                    "« $query »",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Primary500,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }

                        // Cartes recettes avec badge rang IA
                        itemsIndexed(results) { index, recipe ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                RecipeCard(
                                    recipe = recipe,
                                    onRecipeClick = { onRecipeClick(recipe.id.toString()) },
                                    onUsernameClick = { username ->
                                        if (!isLoggedIn) onNavigateToLogin()
                                        else onNavigateToUserProfile(username)
                                    },
                                    isLoggedIn = isLoggedIn,
                                    onNavigateToLogin = onNavigateToLogin,
                                    onLike = onLike,
                                    onUnlike = onUnlike,
                                    onSuperLike = onSuperLike,
                                    onFollow = onFollow,
                                    onUnfollow = onUnfollow,
                                )
                                // Badge rang IA
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(top = 10.dp, start = 10.dp),
                                    shape = RoundedCornerShape(50),
                                    color = Primary500.copy(alpha = 0.92f)
                                ) {
                                    Text(
                                        "#${index + 1}",
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

        // ── Overlay de chargement (équivalent de la modale HTML) ─────────────
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .padding(horizontal = 32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Primary500,
                            modifier = Modifier.size(56.dp),
                            strokeWidth = 4.dp
                        )
                        Text(
                            "Analyse en cours…",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Secondary900,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Notre IA recherche les recettes les plus pertinentes pour vous.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun HowItWorksSection(smartSearchCost : Long?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            "Comment ça fonctionne ?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Secondary900
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Décrivez votre envie culinaire en langage naturel. Notre IA analyse votre requête et trouve les recettes les plus similaires sémantiquement.",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray500,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HowItWorksCard(
                icon = "💬",
                title = "Langage naturel",
                example = "\"quelque chose de léger et rapide pour ce soir\""
            )
            HowItWorksCard(
                icon = "🌿",
                title = "Ingrédients ou régimes",
                example = "\"recette végane avec du tofu et du gingembre\""
            )
            HowItWorksCard(
                icon = "👥",
                title = "Contexte ou occasion",
                example = "\"repas festif pour 6 personnes sans lactose\""
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Chaque recherche intelligente coûte ${if (smartSearchCost != null) "${"%.2f".format(smartSearchCost / 100.0)} €" else "…"} et est débitée de votre portefeuille ForkEat.",
            style = MaterialTheme.typography.labelSmall,
            color = Gray500,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HowItWorksCard(icon: String, title: String, example: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Primary100
            ) {
                Text(
                    icon,
                    modifier = Modifier.padding(8.dp),
                    fontSize = 16.sp
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Secondary900
                )
                Text(
                    example,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
            }
        }
    }
}

// ── Erreur de modération ──────────────────────────────────────────────────────

@Composable
private fun ModerationErrorSection(onNewSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("🛡️", fontSize = 48.sp)
        Text(
            "Requête non autorisée",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Secondary900
        )
        Text(
            "Notre système a détecté un contenu inapproprié. Reformulez votre recherche culinaire.",
            style = MaterialTheme.typography.bodySmall,
            color = Gray500,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onNewSearch,
            colors = ButtonDefaults.buttonColors(containerColor = Primary500),
            shape = RoundedCornerShape(50)
        ) {
            Text("Nouvelle recherche", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Aucun résultat ────────────────────────────────────────────────────────────

@Composable
private fun NoResultsSection(onNewSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("🔍", fontSize = 48.sp)
        Text(
            "Aucune recette trouvée",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Secondary900
        )
        Text(
            "Essayez de reformuler votre requête avec des termes différents.",
            style = MaterialTheme.typography.bodySmall,
            color = Gray500,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onNewSearch) {
            Text(
                "Nouvelle recherche",
                color = Primary500,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
