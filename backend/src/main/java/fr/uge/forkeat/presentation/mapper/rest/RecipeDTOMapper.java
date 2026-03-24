package fr.uge.forkeat.presentation.mapper.rest;

import fr.uge.forkeat.presentation.dto.recipe.*;
import fr.uge.forkeat.presentation.mapper.ImageMapper;
import fr.uge.forkeat.service.model.ImageUpload;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.presentation.dto.recipe.UpdateRecipeRequest;
import fr.uge.forkeat.service.model.recipe.UpdateRecipeCommand;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipe;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipeSummary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public final class RecipeDTOMapper {


    /**
     * Convertit Recipe (service) vers RecipeDTO (présentation)
     */
    public static RecipeDTO toDTO(Recipe recipe) {
        Objects.requireNonNull(recipe);
        return new RecipeDTO(
                recipe.id(),
                recipe.title(),
                recipe.summary(),
                recipe.parentId(),
                recipe.usernameAuthor(),
                recipe.preparationMinutes(),
                recipe.imageUrl(),
                recipe.status().name(),
                toStepDTOs(recipe.stepByStepInstructions()),
                toIngredientDTOs(recipe.ingredients()),
                toAllergenDTOs(recipe.allergens()),
                recipe.dietaries(),
                recipe.createdAt(),
                recipe.updatedAt()
        );
    }

    public static RecipeDTO recipeDTOWithImageUrl(RecipeDTO recipeDTO, String imageUrl) {
        Objects.requireNonNull(recipeDTO);
        return new RecipeDTO(recipeDTO.id(), recipeDTO.title(),
                recipeDTO.summary(), recipeDTO.parentId(),
                recipeDTO.username(), recipeDTO.preparationMinutes(),
                imageUrl, recipeDTO.status(),
                recipeDTO.steps(), recipeDTO.ingredients(),
                recipeDTO.allergens(), recipeDTO.dietaries(),
                recipeDTO.createdAt(), recipeDTO.updatedAt());
    }

    public static RecipeDTO recipeDTOWithUser(RecipeDTO recipeDTO, String username) {
        Objects.requireNonNull(recipeDTO);
        return new RecipeDTO(recipeDTO.id(), recipeDTO.title(),
                recipeDTO.summary(), recipeDTO.parentId(),
                username, recipeDTO.preparationMinutes(),
                recipeDTO.imageUrl(), recipeDTO.status(),
                recipeDTO.steps(), recipeDTO.ingredients(),
                recipeDTO.allergens(), recipeDTO.dietaries(),
                recipeDTO.createdAt(), recipeDTO.updatedAt());
    }


    public static AllergenDTO toDTO(Allergen allergen) {
        Objects.requireNonNull(allergen);
        return new AllergenDTO(
                allergen.id(),
                allergen.name(),
                allergen.severity().name()
        );
    }

    public static CreateRecipeCommand toCommand(CreateRecipeRequest request, String username, ImageUpload image) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(username);
        return new CreateRecipeCommand(
                request.parentId(),
                username,
                request.title(),
                request.summary(),
                request.preparationMinutes(),
                request.draft(),
                image,
                request.imageUrl(),
                toStepsDomain(request.steps()),
                toIngredientsDomain(request.ingredients()),
                toAllergensDomain(request.allergens()),
                request.dietaries()
        );
    }

    public static UpdateRecipeCommand toUpdateCommand(UUID id, UpdateRecipeRequest request, String username, ImageUpload image) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(request);
        Objects.requireNonNull(username);
        return new UpdateRecipeCommand(
                id,
                username,
                request.title(),
                request.summary(),
                request.preparationMinutes(),
                request.draft(),
                image,
                request.imageUrl(),
                toStepsDomain(request.steps()),
                toIngredientsDomain(request.ingredients()),
                toAllergensDomain(request.allergens()),
                request.dietaries()
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
                dto.parentId(),
                //usernameAuthor,
                dto.username(),
                dto.preparationMinutes(),
                dto.imageUrl(),
                dto.status() != null ? RecipeStatus.valueOf(dto.status()) : RecipeStatus.DRAFT,
                toStepsDomain(dto.steps()),
                toIngredientsDomain(dto.ingredients()),
                toAllergensDomain(dto.allergens()),
                dto.dietaries(),
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


    public static PersonalizedRecipeSummaryDTO toSummaryDTO(PersonalizedRecipeSummary p) {
        Objects.requireNonNull(p);
        return new PersonalizedRecipeSummaryDTO(
                p.summary().id(),
                p.summary().title(),
                p.summary().summary(),
                p.summary().imageUrl(),
                p.summary().preparationMinutes(),
                p.summary().authorUsername(),
                p.counts().likeCount(),
                p.counts().superLikeCount(),
                p.counts().followCount(),
                p.interaction().likedByCurrentUser(),
                p.interaction().superLikedByCurrentUser(),
                p.interaction().followedByCurrentUser()
        );
    }

    public static RecipeDetailsDTO toPersonalizedRecipeDTO(PersonalizedRecipe personalizedRecipe) {
        Objects.requireNonNull(personalizedRecipe);
        var recipe = personalizedRecipe.recipe();
        return new RecipeDetailsDTO(
                recipe.id(),
                recipe.title(),
                recipe.summary(),
                recipe.parentId(),
                recipe.usernameAuthor(),
                recipe.preparationMinutes(),
                recipe.imageUrl(),
                recipe.status().name(),
                toStepDTOs(recipe.stepByStepInstructions()),
                toIngredientDTOs(recipe.ingredients()),
                toAllergenDTOs(recipe.allergens()),
                recipe.dietaries(),
                recipe.createdAt(),
                recipe.updatedAt(),
                personalizedRecipe.counts().likeCount(),
                personalizedRecipe.interaction().likedByCurrentUser(),
                personalizedRecipe.counts().superLikeCount(),
                personalizedRecipe.interaction().superLikedByCurrentUser(),
                personalizedRecipe.counts().followCount(),
                personalizedRecipe.interaction().followedByCurrentUser()
        );
    }
}
