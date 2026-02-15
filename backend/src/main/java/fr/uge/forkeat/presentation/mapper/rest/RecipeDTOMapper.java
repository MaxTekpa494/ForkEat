package fr.uge.forkeat.presentation.mapper.rest;

import fr.uge.forkeat.presentation.dto.recipe.*;
import fr.uge.forkeat.service.model.recipe.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public final class RecipeDTOMapper {


    /**
     * Convertit Recipe (service) vers RecipeDTO (présentation)
     */
    public static RecipeDTO toDTO(Recipe recipe, RecipeDTO parentDTO) {
        Objects.requireNonNull(recipe);
        return new RecipeDTO(
                recipe.id(),
                recipe.title(),
                recipe.summary(),
                parentDTO,
                recipe.usernameAuthor(),
                recipe.preparationMinutes(),
                recipe.imageUrl(),
                recipe.status().name(),
                toStepDTOs(recipe.stepByStepInstructions()),
                toIngredientDTOs(recipe.ingredients()),
                toAllergenDTOs(recipe.allergens()),
                recipe.dietaryFlags(),
                recipe.createdAt(),
                recipe.updatedAt()
        );
    }

    public static RecipeDTO recipeDTOWithUser(RecipeDTO recipeDTO, String username) {
        Objects.requireNonNull(recipeDTO);
        return new RecipeDTO(recipeDTO.id(), recipeDTO.title(),
                recipeDTO.summary(), recipeDTO.parent(),
                username, recipeDTO.preparationMinutes(),
                recipeDTO.imageUrl(), recipeDTO.status(),
                recipeDTO.steps(), recipeDTO.ingredients(),
                recipeDTO.allergens(), recipeDTO.dietaryFlags(),
                recipeDTO.createdAt(), recipeDTO.updatedAt());
    }

    public static RecipeDTO toDTO(Recipe recipe) {
        Objects.requireNonNull(recipe);
        return new RecipeDTO(
                recipe.id(),
                recipe.title(),
                recipe.summary(),
                null,
                recipe.usernameAuthor(),
                recipe.preparationMinutes(),
                recipe.imageUrl(),
                recipe.status().name(),
                toStepDTOs(recipe.stepByStepInstructions()),
                toIngredientDTOs(recipe.ingredients()),
                toAllergenDTOs(recipe.allergens()),
                recipe.dietaryFlags(),
                recipe.createdAt(),
                recipe.updatedAt()
        );
    }

    public static AllergenDTO toDTO(Allergen allergen) {
        Objects.requireNonNull(allergen);
        return new AllergenDTO(
                allergen.id(),
                allergen.name(),
                allergen.severity().name()
        );
    }

    /**
     * Convertit RecipeDTO (présentation) vers Recipe (domaine)
     */
    public static Recipe toDomain(RecipeDTO dto) {
        Objects.requireNonNull(dto);
        Objects.requireNonNull(dto.username());
        return new Recipe(
                dto.id() != null ? dto.id() : UUID.randomUUID(),
                dto.title(),
                dto.summary(),
                dto.parent() != null ? dto.parent().id() : null,
                //usernameAuthor,
                dto.username(),
                dto.preparationMinutes(),
                dto.imageUrl(),
                dto.status() != null ? RecipeStatus.valueOf(dto.status()) : RecipeStatus.DRAFT,
                toStepsDomain(dto.steps()),
                toIngredientsDomain(dto.ingredients()),
                toAllergensDomain(dto.allergens()),
                dto.dietaryFlags(),
                dto.createdAt(),
                dto.updatedAt()
        );
    }


    private static List<RecipeStepDTO> toStepDTOs(List<RecipeStep> steps) {
        if (steps == null) {
            return List.of();
        }
        return steps.stream()
                .map(step -> new RecipeStepDTO(step.stepNumber(), step.instruction()))
                .toList();
    }

    private static List<RecipeIngredientDTO> toIngredientDTOs(List<RecipeIngredient> ingredients) {
        if (ingredients == null) {
            return List.of();
        }
        return ingredients.stream()
                .map(ing -> new RecipeIngredientDTO(ing.name(), ing.quantity(), ing.unit()))
                .toList();
    }

    private static List<AllergenDTO> toAllergenDTOs(List<Allergen> allergens) {
        if (allergens == null) {
            return List.of();
        }
        return allergens.stream()
                .map(allergen -> new AllergenDTO(allergen.id(), allergen.name(), allergen.severity().name()
                ))
                .toList();
    }

    private static List<RecipeStep> toStepsDomain(List<RecipeStepDTO> stepDTOs) {
        if (stepDTOs == null) {
            return List.of();
        }
        return stepDTOs.stream()
                .map(dto ->
                        new RecipeStep(dto.stepNumber(), dto.instruction())
                ).toList();
    }

    private static List<RecipeIngredient> toIngredientsDomain(List<RecipeIngredientDTO> ingredientDTOs) {
        if (ingredientDTOs == null) {
            return List.of();
        }
        return ingredientDTOs.stream()
                .map(dto ->
                        new RecipeIngredient(dto.name(), dto.quantity(), dto.unit()))
                .toList();
    }

    private static List<Allergen> toAllergensDomain(List<AllergenDTO> allergenDTOs) {
        if (allergenDTOs == null) {
            return List.of();
        }
        return allergenDTOs.stream()
                .map(dto -> new Allergen(
                        dto.id(),
                        dto.name(),
                        AllergenSeverity.valueOf(dto.severity())
                ))
                .toList();
    }

    public static RecipeWithMetaDataDTO toRecipeWithMetaDataDTO(RecipeWithMetaData recipeWithMetaData, RecipeDTO parentDTO) {
        return new RecipeWithMetaDataDTO(
                recipeWithMetaData.id(),
                recipeWithMetaData.title(),
                recipeWithMetaData.summary(),
                parentDTO,
                recipeWithMetaData.usernameAuthor(),
                recipeWithMetaData.preparationMinutes(),
                recipeWithMetaData.imageUrl(),
                recipeWithMetaData.status().name(),
                toStepDTOs(recipeWithMetaData.stepByStepInstructions()),
                toIngredientDTOs(recipeWithMetaData.ingredients()),
                toAllergenDTOs(recipeWithMetaData.allergens()),
                recipeWithMetaData.dietaryFlags(),
                recipeWithMetaData.createdAt(),
                recipeWithMetaData.updatedAt(),
                recipeWithMetaData.nbLike()
        );
    }

    public static RecipeWithMetaDataDTO toRecipeWithMetaDataDTO(RecipeWithMetaData recipeWithMetaData) {
        return new RecipeWithMetaDataDTO(
                recipeWithMetaData.id(),
                recipeWithMetaData.title(),
                recipeWithMetaData.summary(),
                null,
                recipeWithMetaData.usernameAuthor(),
                recipeWithMetaData.preparationMinutes(),
                recipeWithMetaData.imageUrl(),
                recipeWithMetaData.status().name(),
                toStepDTOs(recipeWithMetaData.stepByStepInstructions()),
                toIngredientDTOs(recipeWithMetaData.ingredients()),
                toAllergenDTOs(recipeWithMetaData.allergens()),
                recipeWithMetaData.dietaryFlags(),
                recipeWithMetaData.createdAt(),
                recipeWithMetaData.updatedAt(),
                recipeWithMetaData.nbLike()
        );
    }
}
