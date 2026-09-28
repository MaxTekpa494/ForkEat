package fr.uge.forkeat.service.model.recipe;

import fr.uge.forkeat.service.model.ImageUpload;

import java.util.List;
import java.util.UUID;

public record CreateRecipeCommand(
        UUID parentId,
        String username,
        String title,
        String summary,
        int preparationMinutes,
        boolean draft,
        ImageUpload image,
        String imageUrl,
        List<RecipeStep> steps,
        List<RecipeIngredient> ingredients,
        List<Allergen> allergens,
        List<String> dietaries
) {}