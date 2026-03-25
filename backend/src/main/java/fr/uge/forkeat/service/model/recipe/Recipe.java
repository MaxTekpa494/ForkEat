package fr.uge.forkeat.service.model.recipe;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record Recipe(UUID id, String title, String summary, UUID parentId,
                     // Ici on met l'ID du parent et non pas Recipe directement
                     // (c'est entre le mapping du Recipe à RecipeDTO qu'on va cherche la recipe parent
                     String usernameAuthor, int preparationMinutes, String imageUrl, RecipeStatus status,
                     List<RecipeStep> stepByStepInstructions, List<RecipeIngredient> ingredients,
                     List<Allergen> allergens,
                     List<String> dietaries, Instant createdAt, Instant updatedAt) {

    public Recipe {
        Objects.requireNonNull(id);
        Objects.requireNonNull(title);
        if (title.isBlank()) {
            throw new IllegalArgumentException("title cannot be empty");
        }
        Objects.requireNonNull(summary);
        Objects.requireNonNull(usernameAuthor);
        Objects.requireNonNull(status);
        if (preparationMinutes < 0) {
            throw new IllegalArgumentException("preparationMinutes cannot be negative");
        }
        stepByStepInstructions = stepByStepInstructions == null ? List.of() : List.copyOf(stepByStepInstructions);
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        allergens = allergens == null ? List.of() : List.copyOf(allergens);
        dietaries = dietaries == null ? List.of() : List.copyOf(dietaries);
        var now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
    }

    public boolean isVariant() {
        return parentId != null;
    }

    public boolean isPublished() {
        return status == RecipeStatus.PUBLISHED;
    }
}
