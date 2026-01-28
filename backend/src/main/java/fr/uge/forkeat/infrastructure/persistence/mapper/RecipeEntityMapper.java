package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.*;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeIngredient;

import java.util.List;
import java.util.Objects;

public final class RecipeEntityMapper {

    private RecipeEntityMapper() {}

    public static Recipe toDomain(RecipeEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Recipe(
                entity.getId(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getParent() != null ? entity.getParent().getId() : null,
                entity.getAuthor().getId(),
                entity.getPreparationMinutes(),
                entity.getImageUrl(),
                entity.getStatus(),
                toRecipeSteps(entity.getStepByStepInstructions()),
                toRecipeIngredients(entity.getIngredients()),
                toAllergenNames(entity.getAllergens()),
                entity.getDietaryFlag(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static RecipeEntity toEntity(Recipe recipe, RecipeEntity recipeParent, UserEntity author){
        Objects.requireNonNull(recipe); // On doit le mettre ou pas ?
        var recipeEntity = new RecipeEntity();
        recipeEntity.setId(recipe.id());
        //recipeEntity.setSource(recipe.source); // Voir si on peut supprimer source
        recipeEntity.setTitle(recipe.title());
        recipeEntity.setSummary(recipe.summary());
        recipeEntity.setParent(recipeParent);
        recipeEntity.setAuthor(author);
        return null;
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

    private static List<String> toAllergenNames(List<RecipeAllergenEntity> entityAllergens) {
        if (entityAllergens == null) {
            return List.of();
        }
        return entityAllergens.stream()
                .map(allergen -> allergen.getAllergen().getName())
                .toList();
    }
}
