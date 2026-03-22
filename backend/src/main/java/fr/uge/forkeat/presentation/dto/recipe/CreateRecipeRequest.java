package fr.uge.forkeat.presentation.dto.recipe;

import java.util.List;
import java.util.UUID;

public record CreateRecipeRequest(
        UUID parentId,
        String title,
        String summary,
        int preparationMinutes,
        boolean draft,
        String imageUrl,
        List<RecipeStepDTO> steps,
        List<RecipeIngredientDTO> ingredients,
        List<AllergenDTO> allergens,
        List<String> dietaries
) {}