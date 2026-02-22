package fr.uge.forkeat.service;

import fr.uge.forkeat.infrastructure.storage.R2StorageService;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RecipeService {
  private final R2StorageService storageService;
  private final RecipePersistence recipePersistence;
  private final Logger logger = LoggerFactory.getLogger(RecipeService.class);

  public RecipeService(RecipePersistence recipePersistence, R2StorageService storageService) {
    this.recipePersistence = recipePersistence;
    this.storageService = storageService;
  }

  @Transactional
  public Recipe createRecipe(Recipe recipe, MultipartFile image) {
    String imageUrl = null;
    if (image != null && !image.isEmpty()) {
      logger.info("Uploading image for recipe {}", recipe.id());
      imageUrl = storageService.uploadImage(image, "recipes");
    }
    var recipeWithImage = new Recipe(
            recipe.id(),
            recipe.title(),
            recipe.summary(),
            recipe.parentId(),
            recipe.usernameAuthor(),
            recipe.preparationMinutes(),
            imageUrl,
            recipe.status(),
            recipe.stepByStepInstructions(),
            recipe.ingredients(),
            recipe.allergens(),
            recipe.dietaryFlags(),
            recipe.createdAt(),
            recipe.updatedAt()
    );
    logger.info("Recipe {} created", recipe.id());
    return recipePersistence.save(recipeWithImage);
  }

  @Transactional
  public void deleteById(UUID id) {
    var recipe = findById(id);
    if(recipe.imageUrl() != null){
      storageService.deleteImage(recipe.imageUrl());
      logger.info("Image deleted for recipe {}", id);
    }
    recipePersistence.deleteById(id);
  }

  public Recipe findById(UUID id) {
    return recipePersistence.findById(id)
            .orElseThrow(() -> new RecipeNotFoundException(id));
  }

  public RecipeWithMetaData findRecipeWithMetaDataById(UUID id) {
      return recipePersistence.findRecipeWithMetaDataById(id)
              .orElseThrow(() -> new RecipeNotFoundException(id));
  }

  public List<Recipe> findByStatus(RecipeStatus status) {
    return recipePersistence.findByStatus(status);
  }

  public PageResult<Recipe> findByStatus(RecipeStatus status, int size, int page) {
    Objects.requireNonNull(status);
    if (size <= 0 || page < 0) {
      throw new IllegalArgumentException("Invalid page or size");
    }
    return recipePersistence.findByStatus(status, size, page);
  }

  public PageResult<Recipe> searchRecipes(RecipeSearchCriteria criteria) {
    Objects.requireNonNull(criteria);
    return recipePersistence.searchRecipes(criteria);
  }

  public List<Allergen> findAllAllergens() {
    return recipePersistence.findAllAllergens();
  }

  public List<String> findAllIngredientNames() {
    return recipePersistence.findAllIngredientNames();
  }

}
