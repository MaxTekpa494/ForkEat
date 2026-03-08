package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.*;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.RecipeSummaryView;
import fr.uge.forkeat.service.model.recipe.Allergen;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeIngredient;
import fr.uge.forkeat.service.model.recipe.projection.RecipeSummary;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public final class RecipeEntityMapper {

  private RecipeEntityMapper() {
  }

  /**
   * Convertit une RecipeEntity vers Recipe
   */
  public static Recipe toDomain(RecipeEntity entity) {
    if (entity == null) {
      return null;
    }
    return new Recipe(
            entity.getId(),
            entity.getTitle(),
            entity.getSummary(),
            entity.getParent() != null ? entity.getParent().getId() : null,
            entity.getAuthor().getUsername(),
            entity.getPreparationMinutes(),
            entity.getImageUrl(),
            entity.getStatus(),
            toRecipeSteps(entity.getStepByStepInstructions()),
            toRecipeIngredients(entity.getIngredients()),
            toAllergens(entity.getAllergens()),
            toDietaries(entity.getDietaries()),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
    );
  }

  public static Allergen toDomain(AllergenEntity entity) {
    if (entity == null) {
      return null;
    }
    return new Allergen(
            entity.getId(),
            entity.getName(),
            entity.getSeverity()
    );
  }

  public static RecipeSummary toDomain(RecipeSummaryView recipeSummaryView) {
    if (recipeSummaryView == null) {
      return null;
    }
    return new RecipeSummary(
            recipeSummaryView.getId(),
            recipeSummaryView.getTitle(),
            recipeSummaryView.getSummary(),
            recipeSummaryView.getImageUrl(),
            recipeSummaryView.getPreparationMinutes(),
            recipeSummaryView.getCreatedAt(),
            recipeSummaryView.getAuthorUsername());
  }

  /**
   * Convertit Recipe vers RecipeEntity
   */
  public static RecipeEntity toEntity(
          Recipe recipe,
          RecipeEntity recipeParent,
          UserEntity author,
          List<AllergenEntity> allergenEntities,
          List<IngredientEntity> ingredientEntities,
          List<DietaryEntity> dietaryEntities
  ) {
    Objects.requireNonNull(recipe);
    Objects.requireNonNull(author);
    Objects.requireNonNull(allergenEntities);
    Objects.requireNonNull(ingredientEntities);
    Objects.requireNonNull(dietaryEntities);

    var entity = new RecipeEntity();
    entity.setId(recipe.id());
    entity.setTitle(recipe.title());
    entity.setSummary(recipe.summary());
    entity.setParent(recipeParent);
    entity.setAuthor(author);
    entity.setPreparationMinutes(recipe.preparationMinutes());
    entity.setImageUrl(recipe.imageUrl());
    entity.setStatus(recipe.status());
    entity.setCreatedAt(recipe.createdAt());
    entity.setUpdatedAt(recipe.updatedAt());

    entity.setStepByStepInstructions(toEntitySteps(recipe.stepByStepInstructions()));
    entity.setAllergens(toRecipeAllergenEntities(recipe.allergens(), allergenEntities, entity));
    entity.setIngredients(toRecipeIngredientEntities(recipe.ingredients(), ingredientEntities, entity));
    entity.setDietaries(toRecipeDietaryEntities(recipe.dietaries(), dietaryEntities, entity));

    return entity;
  }


  private static List<fr.uge.forkeat.service.model.recipe.RecipeStep> toRecipeSteps(List<RecipeStep> entitySteps) {
    if (entitySteps == null) {
      return List.of();
    }
    return entitySteps.stream()
            .map(step -> new fr.uge.forkeat.service.model.recipe.RecipeStep(
                    step.stepNumber(),
                    step.instruction()
            ))
            .toList();
  }

  private static List<RecipeIngredient> toRecipeIngredients(List<RecipeIngredientEntity> entityIngredients) {
    if (entityIngredients == null) {
      return List.of();
    }
    return entityIngredients.stream()
            .map(ing -> new RecipeIngredient(
                    ing.getIngredient().getName(),
                    ing.getQuantity() != null ? ing.getQuantity().doubleValue() : 0.0,
                    ing.getUnit()
            ))
            .toList();
  }

  private static List<Allergen> toAllergens(List<RecipeAllergenEntity> entityAllergens) {
    if (entityAllergens == null) {
      return List.of();
    }
    return entityAllergens.stream()
            .map(recipeAllergen -> new Allergen(
                    recipeAllergen.getAllergen().getId(),
                    recipeAllergen.getAllergen().getName(),
                    recipeAllergen.getAllergen().getSeverity()
            ))
            .toList();
  }


  private static List<String> toDietaries(List<RecipeDietaryEntity> entityDietaryFlags) {
    return entityDietaryFlags.stream()
            .map(dietaryFlag -> dietaryFlag.getDietary().getName())
            .toList();
  }


  public static List<RecipeStep> toEntitySteps(List<fr.uge.forkeat.service.model.recipe.RecipeStep> recipeSteps) {
    if (recipeSteps == null) {
      return List.of();
    }
    return recipeSteps.stream()
            .map(step -> new RecipeStep(
                    step.stepNumber(),
                    step.instruction()
            ))
            .toList();
  }


  public static List<RecipeAllergenEntity> toRecipeAllergenEntities(List<Allergen> allergens, List<AllergenEntity> allergenEntities, RecipeEntity recipe) {
    if (allergens == null || allergens.isEmpty()) {
      return List.of();
    }

    return allergens.stream()
            .map(allergen -> {
              var allergenEntity = allergenEntities.stream()
                      .filter(ae -> ae.getId().equals(allergen.id()))
                      .findFirst()
                      .orElseThrow(() -> new IllegalStateException(
                              "AllergenEntity not found for id: " + allergen.id()
                      ));

              return new RecipeAllergenEntity(recipe, allergenEntity);
            })
            .toList();
  }


  public static List<RecipeIngredientEntity> toRecipeIngredientEntities(
          List<RecipeIngredient> ingredients,
          List<IngredientEntity> ingredientEntities,
          RecipeEntity recipe) {
    if (ingredients == null || ingredients.isEmpty()) {
      return List.of();
    }

    return ingredients.stream()
            .map(ingredient -> {
              var ingredientEntity = ingredientEntities.stream()
                      .filter(ie -> ie.getName().equalsIgnoreCase(ingredient.name()))
                      .findFirst()
                      .orElseThrow(() -> new IllegalStateException(
                              "IngredientEntity not found for name: " + ingredient.name()
                      ));

              return new RecipeIngredientEntity(
                      recipe,
                      ingredientEntity,
                      BigDecimal.valueOf(ingredient.quantity()),
                      ingredient.unit()
              );
            })
            .toList();
  }

  public static List<RecipeDietaryEntity> toRecipeDietaryEntities(List<String> dietaryFlags, List<DietaryEntity> dietaryEntities, RecipeEntity recipe) {
    if (dietaryFlags == null || dietaryFlags.isEmpty()) {
      return List.of();
    }
    return dietaryFlags.stream().map(dietarie -> {
      var dietaryEntity = dietaryEntities.stream()
              .filter(d -> d.getName().equalsIgnoreCase(dietarie))
              .findFirst()
              .orElseThrow(() -> new IllegalStateException(
                      "DietaryEntity not found for name: " + dietarie
              ));
      return new RecipeDietaryEntity(recipe, dietaryEntity);
    }).toList();
  }

}
