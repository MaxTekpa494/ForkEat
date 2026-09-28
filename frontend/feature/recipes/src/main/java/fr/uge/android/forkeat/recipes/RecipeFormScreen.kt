package fr.uge.android.forkeat.recipes

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import fr.uge.android.forkeat.designsystem.theme.Gray500
import fr.uge.android.forkeat.designsystem.theme.Primary500
import fr.uge.android.forkeat.designsystem.theme.Secondary500
import fr.uge.android.forkeat.designsystem.theme.Secondary700
import fr.uge.android.forkeat.designsystem.theme.SurfaceCream
import fr.uge.android.forkeat.designsystem.theme.Typography

@Composable
fun RecipeFormScreen(
    viewModel: RecipeFormViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.resultRecipeId) {
        if (uiState.resultRecipeId != null) {
            onSuccess()
            viewModel.onNavigated()
        }
    }

    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.onImageSelected(uri) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) viewModel.onImageSelected(uiState.cameraUri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.createCameraUri(context)?.let { cameraLauncher.launch(it) }
        }
    }

    Surface(color = SurfaceCream, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary500)
                ) {
                    Text("Retour", color = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Text(viewModel.formTitle, style = Typography.titleLarge, color = Secondary700)
            }

            if (uiState.isLoadingFormData) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary500)
                }
            }

            // Erreur
            uiState.errorMessage?.let { error ->
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF9A9A))
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFC62828),
                        style = Typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Titre
            FormSectionTitle("Titre *")
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nom de la recette") },
                shape = RoundedCornerShape(12.dp),
                colors = formFieldColors(),
                singleLine = true
            )

            // Résumé
            FormSectionTitle("Résumé *")
            OutlinedTextField(
                value = uiState.summary,
                onValueChange = viewModel::onSummaryChange,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Décrivez brièvement votre recette...") },
                shape = RoundedCornerShape(12.dp),
                colors = formFieldColors(),
                maxLines = 5
            )

            // Temps de préparation
            FormSectionTitle("Temps de préparation (minutes) *")
            OutlinedTextField(
                value = uiState.preparationMinutes,
                onValueChange = viewModel::onPrepMinutesChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ex: 30") },
                shape = RoundedCornerShape(12.dp),
                colors = formFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )


            // Image
            FormSectionTitle("Image")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Primary500)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Primary500)
                    Spacer(Modifier.width(4.dp))
                    Text("Galerie", color = Primary500)
                }
                OutlinedButton(
                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Primary500)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Primary500)
                    Spacer(Modifier.width(4.dp))
                    Text("Caméra", color = Primary500)
                }
                // Nouvelle image sélectionnée → priorité sur l'image existante
                val imageToShow = uiState.imageUri ?: uiState.currentImageUrl
                if (imageToShow != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageToShow),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            // Étapes
            FormSectionTitle("Étapes de préparation")
            uiState.steps.forEachIndexed { index, step ->
                StepFormItem(
                    index = index,
                    instruction = step.instruction,
                    onInstructionChange = { viewModel.updateStep(index, it) },
                    onRemove = { viewModel.removeStep(index) }
                )
                if (index < uiState.steps.size - 1) {
                    InsertStepButton(onClick = { viewModel.insertStep(index) })
                }
            }
            OutlinedButton(
                onClick = viewModel::addStep,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Secondary500)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Secondary500)
                Spacer(Modifier.width(4.dp))
                Text("Ajouter une étape", color = Secondary500)
            }

            // Ingrédients
            FormSectionTitle("Ingrédients")
            uiState.ingredients.forEachIndexed { index, ingredient ->
                IngredientFormItem(
                    index = index,
                    ingredient = ingredient,
                    onNameChange = { viewModel.updateIngredientName(index, it) },
                    onQuantityChange = { viewModel.updateIngredientQuantity(index, it) },
                    onUnitChange = { viewModel.updateIngredientUnit(index, it) },
                    onRemove = { viewModel.removeIngredient(index) }
                )
            }
            OutlinedButton(
                onClick = viewModel::addIngredient,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Secondary500)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Secondary500)
                Spacer(Modifier.width(4.dp))
                Text("Ajouter un ingrédient", color = Secondary500)
            }

            // Allergènes
            if (uiState.availableAllergens.isNotEmpty()) {
                FormSectionTitle("Allergènes")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.availableAllergens) { allergen ->
                        val isSelected = uiState.selectedAllergenIds.contains(allergen.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleAllergen(allergen.id) },
                            label = { Text(allergen.name) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFF3CD),
                                selectedLabelColor = Color(0xFFFFA000),
                                containerColor = Color.White,
                                labelColor = Gray500
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color(0xFFE5E7EB),
                                selectedBorderColor = Color(0xFFFFC107),
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }
            }

            // Régimes alimentaires
            if (uiState.availableDietaries.isNotEmpty()) {
                FormSectionTitle("Régimes alimentaires")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.availableDietaries) { dietary ->
                        val isSelected = uiState.selectedDietaries.contains(dietary)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleDietary(dietary) },
                            label = { Text(dietary) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE8F5E9),
                                selectedLabelColor = Color(0xFF2E7D32),
                                containerColor = Color.White,
                                labelColor = Gray500
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color(0xFFE5E7EB),
                                selectedBorderColor = Color(0xFF4CAF50),
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }
            }

            // Boutons submit
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.submitRecipe(draft = true) },
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Primary500)
            ) {
                Text("Enregistrer en brouillon", color = Primary500, style = Typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.submitRecipe(draft = false) },
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary500)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text(viewModel.submitLabel, color = Color.White, style = Typography.titleMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// --- Composants internes ---

@Composable
private fun FormSectionTitle(text: String) {
    Text(text, style = Typography.titleMedium, color = Secondary700)
}

@Composable
private fun formFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = Color(0xFFE5E7EB),
    focusedBorderColor = Primary500,
    unfocusedContainerColor = Color.White,
    focusedContainerColor = Color.White
)

@Composable
private fun InsertStepButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
        IconButton(onClick = onClick, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Insérer une étape",
                tint = Primary500,
                modifier = Modifier.size(14.dp)
            )
        }
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
    }
}

@Composable
private fun StepFormItem(
    index: Int,
    instruction: String,
    onInstructionChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = Primary500, shape = CircleShape, modifier = Modifier.size(28.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("${index + 1}", color = Color.White, style = Typography.labelMedium)
                }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = instruction,
                onValueChange = onInstructionChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Décrivez l'étape...") },
                shape = RoundedCornerShape(8.dp),
                colors = formFieldColors(),
                maxLines = 3
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Supprimer", tint = Gray500)
            }
        }
    }
}

@Composable
private fun IngredientFormItem(
    index: Int,
    ingredient: IngredientState,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${index + 1}.",
                    color = Primary500,
                    style = Typography.labelMedium,
                    modifier = Modifier.width(24.dp)
                )
                OutlinedTextField(
                    value = ingredient.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ingrédient") },
                    shape = RoundedCornerShape(8.dp),
                    colors = formFieldColors(),
                    singleLine = true
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Supprimer", tint = Gray500)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = ingredient.quantity,
                    onValueChange = onQuantityChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Qté") },
                    shape = RoundedCornerShape(8.dp),
                    colors = formFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = ingredient.unit,
                    onValueChange = onUnitChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Unité") },
                    shape = RoundedCornerShape(8.dp),
                    colors = formFieldColors(),
                    singleLine = true
                )
            }
        }
    }
}
