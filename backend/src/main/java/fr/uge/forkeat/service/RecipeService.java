package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.InsufficientFundsException;
import fr.uge.forkeat.service.event.RecipePublishedEvent;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.*;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.recipe.projection.*;
import fr.uge.forkeat.service.model.transaction.Transaction;
import fr.uge.forkeat.service.model.transaction.TransactionStatus;
import fr.uge.forkeat.service.model.transaction.TransactionType;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import fr.uge.forkeat.service.port.EventPublisherPort;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.port.UserIdentityPort;
import fr.uge.forkeat.service.port.StoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RecipeService {
  private final StoragePort storageService;
  private final RecipePersistence recipePersistence;
  private final EventPublisherPort<RecipePublishedEvent> eventPublisher;
  private final WalletPersistence walletPersistence;
  private final AuthenticationPort authPort;
  private final UserIdentityPort userIdentityPort;
  private final Logger logger = LoggerFactory.getLogger(RecipeService.class);
  private static final String FOLDER_STORAGE = "recipes";

  public RecipeService(RecipePersistence recipePersistence, StoragePort storageService, WalletPersistence walletPersistence, AuthenticationPort authPort, EventPublisherPort<RecipePublishedEvent> eventPublisher, UserIdentityPort userIdentityPort) {
    this.recipePersistence = recipePersistence;
    this.storageService = storageService;
    this.eventPublisher = eventPublisher;
    this.walletPersistence = walletPersistence;
    this.authPort = authPort;
    this.userIdentityPort = userIdentityPort;
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

  public PageResult<PersonalizedRecipeSummary> searchRecipes(RecipeSearchCriteria criteria) {
    Objects.requireNonNull(criteria);
    var currentUsername = authPort.extractUsername();

    var page = recipePersistence.searchRecipes(criteria);
    var summaries = page.items();
    if (summaries.isEmpty()) {
      return new PageResult<>(List.of(), page.total());
    }

    var ids = summaries.stream().map(RecipeSummary::id).toList();
    var countsMap = recipePersistence.findRecipeCounts(ids);
    var interactionsMap = currentUsername != null
            ? recipePersistence.findUserRecipeInteractions(ids, currentUsername)
            : Map.<UUID, RecipeUserInteraction>of();

    var personalized = summaries.stream()
            .map(s -> new PersonalizedRecipeSummary(
                    s,
                    countsMap.getOrDefault(s.id(), RecipeCounts.ZERO),
                    interactionsMap.getOrDefault(s.id(), RecipeUserInteraction.NONE)
            ))
            .toList();

    return new PageResult<>(personalized, page.total());
  }

  public List<Allergen> findAllAllergens() {
    return recipePersistence.findAllAllergens();
  }

  public List<String> findAllIngredientNames() {
    return recipePersistence.findAllIngredientNames();
  }

  public List<String> findAllUnitNames() {
    return recipePersistence.findAllUnitNames();
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
    if(!recipePersistence.existRecipe(recipeId)) {
      throw new RecipeNotFoundException(recipeId);
    }
    recipePersistence.likeRecipe(userId, recipeId);
  }

  @Transactional
  public void unlikeRecipe(UUID userId, UUID recipeId) {
    Objects.requireNonNull(userId);
    if(!recipePersistence.existRecipe(recipeId)) {
      throw new RecipeNotFoundException(recipeId);
    }
    recipePersistence.unlikeRecipe(userId, recipeId);
  }

  @Transactional
  public void followRecipe(String username, UUID recipeId) {
    Objects.requireNonNull(username);
    if (!recipePersistence.existRecipe(recipeId)) {
      throw new RecipeNotFoundException(recipeId);
    }
    var userId = userIdentityPort.findIdByUsernameOrThrow(username);
    recipePersistence.followRecipe(userId, recipeId);
  }

  @Transactional
  public void unfollowRecipe(String username, UUID recipeId) {
    Objects.requireNonNull(username);
    if (!recipePersistence.existRecipe(recipeId)) {
      throw new RecipeNotFoundException(recipeId);
    }
    var userId = userIdentityPort.findIdByUsernameOrThrow(username);
    recipePersistence.unfollowRecipe(userId, recipeId);
  }

  @Transactional
  public Recipe updateStatus(UUID id, RecipeStatus status) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(status);
    var updated = recipePersistence.updateStatus(id, status);
    if (status == RecipeStatus.PUBLISHED) {
      logger.info("Recipe {} published, listener will be running and indexing", id);
      eventPublisher.publish(new RecipePublishedEvent(id));
      logger.info("Recipe {} published, listener finished and indexing", id);
    }
    return updated;
  }

  public long countByStatus(RecipeStatus status) {
    Objects.requireNonNull(status);
    return recipePersistence.countByStatus(status);
  }

  @Transactional
    public void superLikeRecipe(UUID userId, UUID recipeId) {
      if(recipePersistence.hasSuperLikedRecipe(userId,  recipeId)){
          return;
      }
      var amount = 100L;

      var wallet = walletPersistence.findByUserId(userId).orElseThrow(() -> new InsufficientFundsException(0L, amount));
      var userBalance = wallet.balance();
      if (userBalance < amount) {
        logger.debug("User {} has not enough balance for this recipe", userId);
        throw new InsufficientFundsException(userBalance, amount);
      }
      var earningsWallet = walletPersistence.getEarningsWallet();
      var redistributionWallet = walletPersistence.getRedistributionWallet();
      var partForEarnings = Math.round(amount * 0.4);
      var partForRedistribution = amount - partForEarnings;
      recipePersistence.superLikeRecipe(userId, recipeId, amount);
      walletPersistence.saveTransaction(new Transaction(UUID.randomUUID(), wallet.id(), null, amount, TransactionType.SUPER_LIKE, Instant.now(), null, TransactionStatus.SUCCEEDED));
      walletPersistence.incrementBalanceById(earningsWallet.id(), partForEarnings);
      walletPersistence.incrementBalanceById(redistributionWallet.id(), partForRedistribution);
  }
}
