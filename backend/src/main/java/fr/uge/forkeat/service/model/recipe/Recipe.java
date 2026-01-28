package fr.uge.forkeat.service.model.recipe;

import java.time.Instant;
import java.util.*;

public record Recipe(
        UUID id, String title,
        String summary, UUID parentId,
        UUID authorId, int preparationMinutes,
        String imageUrl, RecipeStatus status,
        List<RecipeStep> stepByStepInstructions, List<RecipeIngredient> ingredients,
        List<String> allergens, Map<String, Boolean> dietaryFlags,
        Instant createdAt, Instant updatedAt
) {

  public Recipe {
    Objects.requireNonNull(id);
    Objects.requireNonNull(title);
    if (title.isBlank()) {
      throw new IllegalArgumentException("title cannot be empty");
    }
    Objects.requireNonNull(summary);
    Objects.requireNonNull(authorId);
    Objects.requireNonNull(status);
    if (preparationMinutes < 0) {
      throw new IllegalArgumentException("preparationMinutes cannot be negative");
    }
    //Objects.requireNonNull(imageUrl); On ne sait (peut-être des recttes sans iamges ?)
    stepByStepInstructions = stepByStepInstructions == null ? List.of() : List.copyOf(stepByStepInstructions);
    ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
    allergens = allergens == null ? List.of() : List.copyOf(allergens);
    dietaryFlags = dietaryFlags == null ? Map.of() : Map.copyOf(dietaryFlags);
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
