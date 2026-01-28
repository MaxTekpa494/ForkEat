package fr.uge.forkeat.presentation.mapper;

import fr.uge.forkeat.presentation.dto.RecipeDTO;
import fr.uge.forkeat.presentation.dto.RecipeIngredientDTO;
import fr.uge.forkeat.presentation.dto.RecipeStepDTO;
import fr.uge.forkeat.presentation.dto.UserDTO;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.UserService;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeIngredient;
import fr.uge.forkeat.service.model.recipe.RecipeStep;
import fr.uge.forkeat.service.model.user.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public final class RecipeDTOMapper {

    private final UserService userService;
    private final RecipeService recipeService;
    private final UserDTOMapper userDTOMapper;

    public RecipeDTOMapper(UserService userService, RecipeService recipeService, UserDTOMapper userDTOMapper) {
      this.userService = userService;
      this.recipeService = recipeService;
      this.userDTOMapper = userDTOMapper;
    }


    public RecipeDTO toDTO(Recipe recipe) {
        if (recipe == null) {
            return null;
        }
        //var author = userService.findByID(recipe.authorID)
//        return new RecipeDTO(
//                recipe.id(),
//                recipe.title(),
//                recipe.summary(),
//                toParentDTO(recipe.parentId()) ? recipe.isVariant() : null,
//                //UserDTOMapper.toDTO(author),
//                //author.username,
//                recipe.preparationMinutes(),
//                recipe.imageUrl(),
//                recipe.status().name(),
//                toStepDTOs(recipe.stepByStepInstructions()),
//                toIngredientDTOs(recipe.ingredients()),
//                recipe.allergens(),
//                recipe.dietaryFlags(),
//                recipe.createdAt(),
//                recipe.updatedAt()
//        );
        return null;
    }


    private  RecipeDTO toParentDTO(UUID parenID) {
        //var parentRecipe  = recipeService.findByID(parentID);
//        if (parentRecipe == null) {
//            return null;
//        }
//        return new RecipeDTO(
//                parentRecipe.id(),
//                parentRecipe.title(),
//                parentRecipe.summary(),
//                null,
//                UserDTOMapper.toDTO(parentAuthor),
//                parentRecipe.preparationMinutes(),
//                parentRecipe.imageUrl(),
//                parentRecipe.status().name(),
//                toStepDTOs(parentRecipe.stepByStepInstructions()),
//                toIngredientDTOs(parentRecipe.ingredients()),
//                parentRecipe.allergens(),
//                parentRecipe.dietaryFlags(),
//                parentRecipe.createdAt(),
//                parentRecipe.updatedAt()
//        );
        return null;
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
}
