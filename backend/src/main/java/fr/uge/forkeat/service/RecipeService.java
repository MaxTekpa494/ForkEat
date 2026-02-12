package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Allergen;
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
    Objects.requireNonNull(status);
    return recipePersistence.findByStatus(status, size, page);
  }

  public PageResult<Recipe> findByStatusAndSearch(String status, String search, int size, int page) {
    Objects.requireNonNull(status);
    Objects.requireNonNull(search);
    return recipePersistence.findByStatusAndSearch(status, search, size, page);
  }

  public PageResult<Recipe> findByStatusAndSearchAndAllergens(String status, String search, List<String> allergens, int size, int page) {
    Objects.requireNonNull(allergens);
    Objects.requireNonNull(status);
    return recipePersistence.findByStatusAndSearchAndAllergens(status, search, allergens, size, page);
  }

  public List<Allergen> findAllAllergens() {
    return recipePersistence.findAllAllergens();
  }

  public Recipe createRecipe(Recipe recipe) {
    return recipePersistence.save(recipe);
  }

}
