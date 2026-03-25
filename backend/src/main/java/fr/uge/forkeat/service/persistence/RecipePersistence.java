package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipeSummary;
import fr.uge.forkeat.service.model.recipe.projection.RecipeSummary;
import fr.uge.forkeat.service.model.recipe.projection.AuthorRecipeSummary;
import fr.uge.forkeat.service.model.recipe.projection.RecipeCounts;
import fr.uge.forkeat.service.model.recipe.projection.UserRecipeStats;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface RecipePersistence {

  Optional<Recipe> findById(UUID id);

  boolean existRecipe(UUID id);

  List<Recipe> findByStatus(RecipeStatus status);

  PageResult<Recipe> findByStatus(RecipeStatus status, int size, int page);

  PageResult<Recipe> getRecipesToModerate(UUID authorId, int size, int page);

  boolean isAuthor(UUID recipeId, UUID authorId);

  PageResult<RecipeSummary> searchRecipes(RecipeSearchCriteria criteria);

  List<Recipe> findByAuthorId(UUID authorId);

  List<Recipe> findByAuthorUsername(String authorUsername);

  List<Allergen> findAllAllergens();

  List<String> findAllIngredientNames();

  List<String> findAllUnitNames();

  List<String> findAllDietaryNames();

  Recipe save(Recipe recipe);

  Recipe update(UUID id, Recipe recipe);

  void deleteById(UUID id);

  boolean isImageUrlUsedByOtherRecipes(UUID excludeRecipeId, String imageUrl);

  void reparentVariants(UUID deletedId, UUID newParentId);

  void reassignRecipesToUser(UUID fromUserId, UUID toUserId);

  Recipe updateStatus(UUID id, RecipeStatus status);

  long countByStatus(RecipeStatus status);

  PageResult<RecipeSummary> searchPersonalizedFeedRecipes(String username, Instant beforeTime, int nbPage);

  // Alors ici on ne fait pas Page<RecipeSummary> parce qu'on
  // n'est pas sensé renvoyer plein de recette quand c'est du RAG...
  List<RecipeSummary> findSummariesByIds(List<UUID> ids);

  PageResult<RecipeSummary> findUserRecipeSummaries(String username, RecipeStatus status, int size, int page);

  RecipeCounts findRecipeCounts(UUID recipeId);

  Map<UUID, RecipeCounts> findRecipeCounts(List<UUID> recipeIds);

  RecipeUserInteraction findUserRecipeInteraction(UUID recipeId, UUID userId);

  Map<UUID, RecipeUserInteraction> findUserRecipeInteractions(List<UUID> recipeIds, UUID userId);

  long countByAuthorUsername(String username);

  Optional<PersonalizedRecipeSummary> findTopLikedPublishedRecipe();

  void likeRecipe(UUID userId, UUID recipeId);

  void unlikeRecipe(UUID userId, UUID recipeId);

  void followRecipe(UUID userId, UUID recipeId);

  void unfollowRecipe(UUID userId, UUID recipeId);

  void superLikeRecipe(UUID userId, UUID recipeId, long amount, UUID promotionId, boolean isBonusFree, long redistAmountCents);

  boolean hasSuperLikedRecipe(UUID userId, UUID recipeId);

  PageResult<AuthorRecipeSummary> findRecipesByAuthor(UUID authorId, RecipeStatus status, int page, int size);

  UserRecipeStats countRecipesByAuthorGroupedByStatus(UUID authorId);
}