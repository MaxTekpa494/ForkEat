package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.recipe.projection.RecipeSummary;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface RecipePersistence {

  Optional<Recipe> findById(UUID id);

  List<Recipe> findByStatus(RecipeStatus status);

  PageResult<Recipe> findByStatus(RecipeStatus status, int size, int page);

  PageResult<Recipe> searchRecipes(RecipeSearchCriteria criteria);

  List<Recipe> findByAuthorId(UUID authorId);

  List<Allergen> findAllAllergens();

  List<String> findAllIngredientNames();

  Recipe save(Recipe recipe);

  void deleteById(UUID id);

  PageResult<RecipeSummary> findRecipeSummaries(String username, RecipeStatus status, int size, int page);

  Map<UUID, RecipeUserInteraction> findUserRecipeInteractions(List<UUID> recipeIds, String currentUsername);

  long countByAuthorUsername(String username);

}
