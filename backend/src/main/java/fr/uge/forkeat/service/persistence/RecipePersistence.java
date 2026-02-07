package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Recipe;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipePersistence {

  Optional<Recipe> findById(UUID id);

  List<Recipe> findByStatus(String status);

  PageResult<Recipe> findByStatus(String status, int size, int page);

  List<Recipe> findByAuthorId(UUID authorId);

  Recipe save(Recipe recipe);

  void deleteById(UUID id);

}
