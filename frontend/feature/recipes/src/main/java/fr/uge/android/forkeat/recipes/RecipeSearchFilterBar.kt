package fr.uge.android.forkeat.recipes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.uge.android.forkeat.designsystem.theme.Primary500
import fr.uge.android.forkeat.designsystem.theme.Secondary700
import fr.uge.android.forkeat.designsystem.theme.Gray500

@Composable
fun RecipeSearchFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    availableAllergens: List<String>,
    selectedAllergens: Set<String>,
    onAllergenToggle: (String) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasActiveFilters = searchQuery.isNotBlank() || selectedAllergens.isNotEmpty()
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            placeholder = { Text("Rechercher une recette...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Effacer")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedBorderColor = Primary500,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearchSubmit()
                    focusManager.clearFocus()
                }
            )
        )

        // Allergen filters
        AnimatedVisibility(visible = availableAllergens.isNotEmpty()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Exclure les allergènes :",
                        style = MaterialTheme.typography.labelMedium,
                        color = Secondary700,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (hasActiveFilters) {
                        TextButton(onClick = onClearFilters) {
                            Text(
                                "Réinitialiser",
                                fontSize = 12.sp,
                                color = Primary500
                            )
                        }
                    }
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(availableAllergens) { allergen ->
                        val isSelected = selectedAllergens.contains(allergen)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onAllergenToggle(allergen) },
                            label = { Text(allergen, fontSize = 13.sp) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary500,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Gray500
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color(0xFFE5E7EB),
                                selectedBorderColor = Primary500,
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }
            }
        }
    }
}
