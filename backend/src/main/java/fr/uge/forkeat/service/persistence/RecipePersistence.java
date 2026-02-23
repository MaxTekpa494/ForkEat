package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipePersistence {

  Optional<Recipe> findById(UUID id);

  List<Recipe> findByStatus(RecipeStatus status);

  PageResult<Recipe> findByStatus(RecipeStatus status, int size, int page);

  PageResult<Recipe> searchRecipes(RecipeSearchCriteria criteria);

  List<Recipe> findByAuthorId(UUID authorId);

  List<Recipe> findByAuthorUsername(String authorUsername);

  List<Allergen> findAllAllergens();

  List<String> findAllIngredientNames();

  Recipe save(Recipe recipe);

  Recipe update(UUID id, Recipe recipe);

  void deleteById(UUID id);

  Optional<RecipeWithMetaData> findRecipeWithMetaDataById(UUID recipeId);

}
