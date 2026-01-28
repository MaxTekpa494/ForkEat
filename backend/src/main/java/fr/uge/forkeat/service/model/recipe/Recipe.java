package fr.uge.forkeat.service.model.recipe;

import java.time.Instant;
import java.util.*;

public record Recipe(
        UUID id,
        String title,
        String summary,
        UUID parentId,
        UUID authorId,
        int preparationMinutes,
        String imageUrl,
        RecipeStatus status,
        List<RecipeStep> stepByStepInstructions,
        List<RecipeIngredient> ingredients,
        List<String> allergens,
        Map<String, Boolean> dietaryFlags,
        Instant createdAt,
        Instant updatedAt
) {

  public boolean isVariant() {
    return parentId != null;
  }

  public boolean isPublished() {
    return status == RecipeStatus.PUBLISHED;
  }

  public static Builder with(){
    return new Builder();
  }


  // Le record, je ne peux pas le mettre en package mais j'ai vraiment besion de ce builder donc ...
  public static class Builder {
    private UUID id;
    private String title;
    private String summary;
    private UUID parentId;
    private UUID authorId;
    private int preparationMinutes;
    private String imageUrl;
    private RecipeStatus status = RecipeStatus.DRAFT;
    private final ArrayList<RecipeStep> stepByStepInstructions = new ArrayList<>();
    private final ArrayList<RecipeIngredient> ingredients = new ArrayList<>();
    private final ArrayList<String> allergens = new ArrayList<>();
    private final HashMap<String, Boolean> dietaryFlags = new HashMap<>();
    private Instant createdAt;
    private Instant updatedAt;

//    public Builder id(UUID id) {
//      Objects.requireNonNull(id);
//      this.id = id;
//      return this;
//    }

    public Builder title(String title) {
      this.title = Objects.requireNonNull(title);
      return this;
    }

    public Builder summary(String summary) {
      this.summary = Objects.requireNonNull(summary);
      return this;
    }

    public Builder parentId(UUID parentId) {
      this.parentId = Objects.requireNonNull(parentId);
      return this;
    }

    public Builder authorId(UUID authorId) {
      this.authorId = Objects.requireNonNull(authorId);
      return this;
    }

    public Builder preparationMinutes(int preparationMinutes) {
      if(preparationMinutes < 0){
        throw new IllegalArgumentException("Recipe : preparationMinutes < 0");
      }
      this.preparationMinutes = preparationMinutes;
      return this;
    }

    public Builder imageUrl(String imageUrl) {
      this.imageUrl = Objects.requireNonNull(imageUrl);
      return this;
    }

    public Builder status(RecipeStatus status) {
      this.status = Objects.requireNonNull(status);
      return this;
    }

    public Builder stepByStepInstructions(List<RecipeStep> stepByStepInstructions) {
      Objects.requireNonNull(stepByStepInstructions);
      stepByStepInstructions.forEach(this::addStep);
      return this;
    }

    public Builder addStep(RecipeStep step) {
      Objects.requireNonNull(step);
      this.stepByStepInstructions.add(step);
      return this;
    }

    public Builder ingredients(List<RecipeIngredient> ingredients) {
      Objects.requireNonNull(ingredients);
      ingredients.forEach(this::addIngredient);
      return this;
    }

    public Builder addIngredient(RecipeIngredient ingredient) {
      Objects.requireNonNull(ingredient);
      this.ingredients.add(ingredient);
      return this;
    }

    public Builder allergens(List<String> allergens) {
      Objects.requireNonNull(allergens);
      allergens.forEach(this::addAllergen);
      return this;
    }

    public Builder addAllergen(String allergen) {
      Objects.requireNonNull(allergen);
      this.allergens.add(allergen);
      return this;
    }

    public Builder dietaryFlags(Map<String, Boolean> dietaryFlags) {
      Objects.requireNonNull(dietaryFlags);
      dietaryFlags.forEach(this::addDietaryFlag);
      return this;
    }

    public Builder addDietaryFlag(String flag, boolean value) {
      Objects.requireNonNull(flag);
      this.dietaryFlags.put(flag, value);
      return this;
    }

//    public Builder createdAt(Instant createdAt) {
//      this.createdAt = Objects.requireNonNull(createdAt);
//      return this;
//    }
//
//    public Builder updatedAt(Instant updatedAt) {
//      this.updatedAt = Objects.requireNonNull(updatedAt);
//      return this;
//    }

    public Recipe build() {
      Instant now = Instant.now();
      if (createdAt == null) {
        createdAt = now;
      }
      if (updatedAt == null) {
        updatedAt = now;
      }

      if (id == null) {
        id = UUID.randomUUID();
      }

      if (title == null || title.isBlank()) {
        throw new IllegalStateException("Recipe title cannot be null or empty");
      }
      if (authorId == null) {
        throw new IllegalStateException("Recipe must have an author");
      }

      return new Recipe(
              id,
              title,
              summary,
              parentId,
              authorId,
              preparationMinutes,
              imageUrl,
              status,
              List.copyOf(stepByStepInstructions),
              List.copyOf(ingredients),
              List.copyOf(allergens),
              Map.copyOf(dietaryFlags),
              createdAt,
              updatedAt
      );
    }
  }
}
