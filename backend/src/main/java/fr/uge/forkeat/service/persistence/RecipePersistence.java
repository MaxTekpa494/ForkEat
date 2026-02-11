package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Allergen;
import fr.uge.forkeat.service.model.recipe.Recipe;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipePersistence {

  Optional<Recipe> findById(UUID id);

  List<Recipe> findByStatus(String status);

  PageResult<Recipe> findByStatus(String status, int size, int page);

  PageResult<Recipe> findByStatusAndSearch(String status, String search, int size, int page);

  PageResult<Recipe> findByStatusAndSearchAndAllergens(String status, String search, List<String> allergens, int size, int page);

  List<Recipe> findByAuthorId(UUID authorId);

  List<Allergen> findAllAllergens();

  Recipe save(Recipe recipe);

  void deleteById(UUID id);

}
