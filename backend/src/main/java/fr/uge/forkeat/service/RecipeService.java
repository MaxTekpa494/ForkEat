package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.ImageUpload;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipe;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import fr.uge.forkeat.service.port.StoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RecipeService {
  private final StoragePort storageService;
  private final RecipePersistence recipePersistence;
  private final Logger logger = LoggerFactory.getLogger(RecipeService.class);
  private static final String FOLDER_STORAGE = "recipes";

  public RecipeService(RecipePersistence recipePersistence, StoragePort storageService) {
    this.recipePersistence = recipePersistence;
    this.storageService = storageService;
  }

  @Transactional
  public Recipe createRecipe(Recipe recipe, ImageUpload image) {
    var imageUrl = recipe.imageUrl();
    if (image != null) {
      logger.info("Uploading image for recipe {}", recipe.id());
      imageUrl = storageService.uploadImage(image, FOLDER_STORAGE);
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
            recipe.dietaries(),
            recipe.createdAt(),
            recipe.updatedAt()
    );
    logger.info("Recipe {} created", recipe.id());
    return recipePersistence.save(recipeWithImage);
  }

  @Transactional
  public Recipe updateRecipe(UUID id, Recipe updatedRecipe, ImageUpload image) {
    var existingRecipe = findById(id);

    var imageUrl = existingRecipe.imageUrl();
    if (image != null) {
      if (existingRecipe.imageUrl() != null) {
        storageService.deleteImage(existingRecipe.imageUrl());
        logger.info("Old image deleted for recipe {}", id);
      }
      imageUrl = storageService.uploadImage(image, FOLDER_STORAGE);
      logger.info("New image uploaded for recipe {}", id);
    }

    var recipeToSave = new Recipe(
            id,
            updatedRecipe.title(),
            updatedRecipe.summary(),
            existingRecipe.parentId(),
            existingRecipe.usernameAuthor(),
            updatedRecipe.preparationMinutes(),
            imageUrl,
            updatedRecipe.status(),
            updatedRecipe.stepByStepInstructions(),
            updatedRecipe.ingredients(),
            updatedRecipe.allergens(),
            updatedRecipe.dietaries(),
            existingRecipe.createdAt(),
            updatedRecipe.updatedAt()
    );

    logger.info("Recipe {} updated", id);
    return recipePersistence.update(id, recipeToSave);
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

  public PersonalizedRecipe findPersonalizedRecipeById(UUID id, String currentUsername) {
      Objects.requireNonNull(id);
      var recipe = recipePersistence.findById(id)
              .orElseThrow(() -> new RecipeNotFoundException(id));
      var counts = recipePersistence.findRecipeCounts(id);
      var interaction = currentUsername != null
              ? recipePersistence.findUserRecipeInteraction(id, currentUsername)
              : RecipeUserInteraction.NONE;
      return new PersonalizedRecipe(recipe, counts, interaction);
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

  public List<String> findAllDietaryNames() {
    return recipePersistence.findAllDietaryNames();
  }

  public List<Recipe> findByAuthorUsername(String authorUsername) {
    return recipePersistence.findByAuthorUsername(authorUsername);
  }

  @Transactional
  public void likeRecipe(UUID userId, UUID recipeId) {
    Objects.requireNonNull(userId);
    findById(recipeId); // vérifie que la recette existe
    recipePersistence.likeRecipe(userId, recipeId);
  }

  @Transactional
  public void unlikeRecipe(UUID userId, UUID recipeId) {
    Objects.requireNonNull(userId);
    findById(recipeId); // vérifie que la recette existe
    recipePersistence.unlikeRecipe(userId, recipeId);
  }

  @Transactional
  public Recipe updateStatus(UUID id, RecipeStatus status) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(status);
    return recipePersistence.updateStatus(id, status);
  }

  public long countByStatus(RecipeStatus status) {
    Objects.requireNonNull(status);
    return recipePersistence.countByStatus(status);
  }

}
