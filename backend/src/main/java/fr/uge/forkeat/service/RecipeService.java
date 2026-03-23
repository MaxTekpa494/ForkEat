package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.InsufficientFundsException;
import fr.uge.forkeat.service.event.RecipePublishedEvent;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.exception.RecipeOwnershipException;
import fr.uge.forkeat.service.exception.VariantCreationNotAllowedException;
import fr.uge.forkeat.service.model.*;
import fr.uge.forkeat.service.exception.WalletNotFoundException;
import fr.uge.forkeat.service.model.ImageUpload;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.recipe.projection.*;
import fr.uge.forkeat.service.model.transaction.Transaction;
import fr.uge.forkeat.service.model.transaction.TransactionStatus;
import fr.uge.forkeat.service.model.transaction.TransactionType;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipe;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;
import fr.uge.forkeat.service.persistence.*;
import fr.uge.forkeat.service.port.EventPublisherPort;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.port.UserIdentityPort;
import fr.uge.forkeat.service.port.StoragePort;
import fr.uge.forkeat.service.security.SecurityService;
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
  private final RecipeDiffService recipeDiffService;
  private final EventPublisherPort<RecipePublishedEvent> eventPublisher;
  private final WalletPersistence walletPersistence;
  private final AuthenticationPort authPort;
  private final UserIdentityPort userIdentityPort;
  private final SuperLikeConfigPersistence superLikeConfigPersistence;
  private final PromotionPersistence promotionPersistence;
  private final SecurityService securityService;
  private final PlatformWalletPersistence platformWalletPersistence;
  private final Logger logger = LoggerFactory.getLogger(RecipeService.class);
  private static final String FOLDER_STORAGE = "recipes";

  public RecipeService(RecipePersistence recipePersistence, RecipeDiffService recipeDiffService,
                       StoragePort storageService,
                       WalletPersistence walletPersistence,
                       AuthenticationPort authPort,
                       SuperLikeConfigPersistence superLikeConfigPersistence,
                       PromotionPersistence promotionPersistence,
                       PlatformWalletPersistence platformWalletPersistence,
                       EventPublisherPort<RecipePublishedEvent> eventPublisher, UserIdentityPort userIdentityPort,
                       SecurityService securityService) {
    this.recipePersistence = recipePersistence;
    this.recipeDiffService = recipeDiffService;
    this.storageService = storageService;
    this.eventPublisher = eventPublisher;
    this.walletPersistence = walletPersistence;
    this.superLikeConfigPersistence = superLikeConfigPersistence;
    this.promotionPersistence = promotionPersistence;
    this.platformWalletPersistence = platformWalletPersistence;
    this.authPort = authPort;
    this.userIdentityPort = userIdentityPort;
    this.securityService = securityService;
  }

  @Transactional
  public Recipe createRecipe(CreateRecipeCommand command) {
    userIdentityPort.findIdByUsernameOrThrow(command.username());
    if (command.draft() && (command.title() == null || command.title().isBlank())) {
      throw new IllegalArgumentException("Draft recipe must have a non-empty title");
    }
    if (command.parentId() != null) {
      var parent = recipePersistence.findById(command.parentId())
              .orElseThrow(() -> new RecipeNotFoundException(command.parentId()));
      if (parent.status() != RecipeStatus.PUBLISHED) {
        throw new VariantCreationNotAllowedException(command.parentId());
      }
    }
    var status = command.draft() ? RecipeStatus.DRAFT : RecipeStatus.PENDING_REVIEW;
    var now = java.time.Instant.now();
    var imageUrl = command.image() != null
            ? storageService.uploadImage(command.image(), FOLDER_STORAGE)
            : command.imageUrl();
    var recipe = new Recipe(
            UUID.randomUUID(),
            command.title(),
            command.summary(),
            command.parentId(),
            command.username(),
            command.preparationMinutes(),
            imageUrl,
            status,
            command.steps(),
            command.ingredients(),
            command.allergens(),
            command.dietaries(),
            now,
            now
    );
    logger.info("Recipe {} created", recipe.id());
    return recipePersistence.save(recipe);
  }

  @Transactional
  public Recipe updateRecipe(UUID id, Recipe updatedRecipe, ImageUpload image) {
    var existingRecipe = findById(id);
    if(!securityService.canUpdateRecipe(existingRecipe)){
        //Forbidden Exception de MAX
        throw new RecipeOwnershipException(id, authPort.extractUsername());
    }

    var imageUrl = existingRecipe.imageUrl();
    if (image != null) {
      if (existingRecipe.imageUrl() != null && !recipePersistence.isImageUrlUsedByOtherRecipes(id, existingRecipe.imageUrl())) {
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
      if(!securityService.canDeleteRecipe(recipe)){
          throw new RecipeOwnershipException(id, authPort.extractUsername());
      }
    if(recipe.imageUrl() != null && !recipePersistence.isImageUrlUsedByOtherRecipes(id, recipe.imageUrl())){
      storageService.deleteImage(recipe.imageUrl());
      logger.info("Image deleted for recipe {}", id);
    }
    recipePersistence.reparentVariants(id, recipe.parentId());
    recipePersistence.deleteById(id);
  }

  public Recipe findById(UUID id) {
    var recipe = recipePersistence.findById(id)
            .orElseThrow(() -> new RecipeNotFoundException(id));
    if (!recipe.isPublished() && !authPort.isAdmin() && !authPort.isModerator()) {
      var currentUsername = authPort.extractUsername();
      if (currentUsername == null || !currentUsername.equals(recipe.usernameAuthor())) {
        throw new RecipeNotFoundException(id);
      }
    }
    return recipe;
  }

  public PersonalizedRecipe findPersonalizedRecipeById(UUID id, String currentUsername) {
      Objects.requireNonNull(id);
      var recipe = recipePersistence.findById(id)
              .orElseThrow(() -> new RecipeNotFoundException(id));
      if (!recipe.isPublished() && !authPort.isAdmin() && !authPort.isModerator()
              && (currentUsername == null || !currentUsername.equals(recipe.usernameAuthor()))) {
          throw new RecipeNotFoundException(id);
      }
      var counts = recipePersistence.findRecipeCounts(id);
      var interaction = currentUsername != null
              ? recipePersistence.findUserRecipeInteraction(id, currentUsername)
              : RecipeUserInteraction.NONE;
      RecipeDiff diff = null;
      if (recipe.isVariant()) {
          var parent = recipePersistence.findById(recipe.parentId())
                  .orElseThrow(() -> new RecipeNotFoundException(recipe.parentId()));
          if (parent.isPublished()) {
              diff = recipeDiffService.computeDiff(parent, recipe);
          }
      }
      return new PersonalizedRecipe(recipe, counts, interaction, diff);
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

  public PageResult<Recipe> getRecipesToModerate(String authorUsername, int size, int page) {
    Objects.requireNonNull(authorUsername);
    if (size <= 0 || page < 0) {
      throw new IllegalArgumentException("Invalid page or size");
    }
    var authorId = userIdentityPort.findIdByUsernameOrThrow(authorUsername);
    return recipePersistence.getRecipesToModerate(authorId, size, page);
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

    public PageResult<PersonalizedRecipeSummary> getPersonalizedFeedRecipes(Instant instant, int nbPage) {
        var currentUsername = authPort.extractUsername();
        var page = recipePersistence.searchPersonalizedFeedRecipes(currentUsername, instant, nbPage);
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

  public AuthorRecipesPage findRecipesByAuthor(String username, RecipeStatus status, int page, int size) {
    Objects.requireNonNull(username);
    Objects.requireNonNull(status);
    if (size <= 0 || page < 0) {
      throw new IllegalArgumentException("Invalid page or size");
    }
    var authorId = userIdentityPort.findIdByUsernameOrThrow(username);
    var stats = recipePersistence.countRecipesByAuthorGroupedByStatus(authorId);
    var recipes = recipePersistence.findRecipesByAuthor(authorId, status, page, size);
    return new AuthorRecipesPage(stats, recipes);
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
      if (recipePersistence.hasSuperLikedRecipe(userId, recipeId)) return;

      var wallet = walletPersistence.loadWalletWithLock(userId)
              .orElseThrow(() -> new WalletNotFoundException(userId));

      var pricing = resolvePricing(userId);

      if (wallet.balance() < pricing.effectivePrice()) {
          logger.debug("User {} has insufficient balance for super-like (balance={}, required={})",
                  userId, wallet.balance(), pricing.effectivePrice());
          throw new InsufficientFundsException(wallet.balance(), pricing.effectivePrice());
      }

      applyWalletMovements(wallet.id(), recipeId, pricing);
      recipePersistence.superLikeRecipe(userId, recipeId, pricing.effectivePrice(), pricing.promotionId(), pricing.isBonusFree(), pricing.redistPart());
      walletPersistence.saveTransaction(new Transaction(UUID.randomUUID(), wallet.id(), null, pricing.effectivePrice(), TransactionType.SUPER_LIKE, Instant.now(), null, TransactionStatus.SUCCEEDED));
  }

  private record SuperLikePricing(long fullPrice, long effectivePrice, UUID promotionId, boolean isBonusFree, long earningsPart, long redistPart) {}

  private SuperLikePricing resolvePricing(UUID userId) {
      var config = superLikeConfigPersistence.get();
      var activePromotion = promotionPersistence.findActiveAt(Instant.now());

      long fullPrice;
      UUID promotionId = null;
      boolean isBonusFree = false;

      if (activePromotion.isPresent()) {
          var promo = activePromotion.get();
          promotionId = promo.id();
          fullPrice = promo.priceCents();
          if (promo.bonusEveryN() != null) {
              int paidCount = promotionPersistence.countPaidSuperLikesByUserAndPromotion(userId, promo.id());
              if (paidCount > 0 && paidCount % promo.bonusEveryN() == 0) {
                  isBonusFree = true;
              }
          }
      } else {
          fullPrice = config.priceCents();
      }

      long earningsPart = Math.round(fullPrice * config.earningsRatio().doubleValue());
      long redistPart = fullPrice - earningsPart;
      long effectivePrice = isBonusFree ? 0L : fullPrice;

      return new SuperLikePricing(fullPrice, effectivePrice, promotionId, isBonusFree, earningsPart, redistPart);
  }

  private void applyWalletMovements(UUID walletId, UUID recipeId, SuperLikePricing pricing) {
      var earningsWallet = walletPersistence.getEarningsWallet();
      var redistributionWallet = walletPersistence.getRedistributionWallet();

      if (pricing.isBonusFree()) {
          walletPersistence.decrementBalanceById(earningsWallet.id(), pricing.redistPart());
          walletPersistence.incrementBalanceById(redistributionWallet.id(), pricing.redistPart());
          platformWalletPersistence.recordTransaction(PlatformWalletType.EARNINGS, -pricing.redistPart(), "BONUS_FINANCED", recipeId);
          platformWalletPersistence.recordTransaction(PlatformWalletType.REDISTRIBUTION, pricing.redistPart(), "SUPER_LIKE_REDISTRIBUTION", recipeId);
          logger.debug("Free super-like on recipe {} (promo {}): EARNINGS -{}, REDISTRIBUTION +{}",
                  recipeId, pricing.promotionId(), pricing.redistPart(), pricing.redistPart());
      } else {
          walletPersistence.decrementBalanceById(walletId, pricing.effectivePrice());
          walletPersistence.incrementBalanceById(earningsWallet.id(), pricing.earningsPart());
          walletPersistence.incrementBalanceById(redistributionWallet.id(), pricing.redistPart());
          platformWalletPersistence.recordTransaction(PlatformWalletType.EARNINGS, pricing.earningsPart(), "SUPER_LIKE_EARNED", recipeId);
          platformWalletPersistence.recordTransaction(PlatformWalletType.REDISTRIBUTION, pricing.redistPart(), "SUPER_LIKE_REDISTRIBUTION", recipeId);
          logger.debug("Paid super-like on recipe {} (price={}, promo={}): EARNINGS +{}, REDISTRIBUTION +{}",
                  recipeId, pricing.effectivePrice(), pricing.promotionId(), pricing.earningsPart(), pricing.redistPart());
      }
  }
}
