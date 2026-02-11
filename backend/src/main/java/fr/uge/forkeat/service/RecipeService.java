package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RecipeService {
  private final RecipePersistence recipePersistence;

  public RecipeService(RecipePersistence recipePersistence) {
    this.recipePersistence = Objects.requireNonNull(recipePersistence);
  }

  public Recipe findById(UUID id) {
    return recipePersistence.findById(id)
            .orElseThrow(() -> new RecipeNotFoundException(id));
  }

  public List<Recipe> findByStatus(String status) {
    return recipePersistence.findByStatus(status);
  }

  public PageResult<Recipe> findByStatus(String status, int size, int page) {
    return recipePersistence.findByStatus(status, size, page);
  }

  public Recipe createRecipe(Recipe recipe) {
    return recipePersistence.save(recipe);
  }

}
