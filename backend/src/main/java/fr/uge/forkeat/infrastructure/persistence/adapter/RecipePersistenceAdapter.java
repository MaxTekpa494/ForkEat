package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRepository;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public final class RecipePersistenceAdapter implements RecipePersistence {

  private final RecipeRepository recipeRepository;

  public RecipePersistenceAdapter(RecipeRepository recipeRepository) {
    this.recipeRepository = recipeRepository; // Est-ce qu'on doit mettre les required non null
  }


  @Override
  public Optional<Recipe> findById(UUID id) {
    //return recipeRepository.findById(id).map(this::toDomain);
    return Optional.empty();
  }

  @Override
  public List<Recipe> findByStatus(String status) {
//    return recipeRepository.findByStatus(RecipeStatus.valueOf(status))
//            .stream()
//            .map(this::toDomain)
//            .toList();
    return List.of();
  }

  @Override
  public List<Recipe> findByAuthorId(UUID authorId) {
//    return recipeRepository.findByAuthorId(authorId)
//            .stream()
//            .map(this::toDomain)
//            .toList();
    return List.of();

  }

  @Override
  public Recipe save(Recipe recipe) {
//    var entity = toEntity(recipe);
//    var saved = recipeRepository.save(entity);
//    return toDomain(saved);
    return null;
  }

  @Override
  public void deleteById(UUID id) {
//    recipeRepository.deleteById(id);
  }
}
