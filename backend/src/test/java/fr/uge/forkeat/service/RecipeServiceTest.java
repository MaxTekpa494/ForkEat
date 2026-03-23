package fr.uge.forkeat.service;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.service.event.RecipePublishedEvent;
import fr.uge.forkeat.service.exception.InsufficientFundsException;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.ImageUpload;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.recipe.projection.AuthorRecipeSummary;
import fr.uge.forkeat.service.model.recipe.projection.RecipeCounts;
import fr.uge.forkeat.service.model.recipe.projection.RecipeSummary;
import fr.uge.forkeat.service.model.recipe.projection.UserRecipeStats;
import fr.uge.forkeat.service.model.superlike.SuperLikeConfig;
import fr.uge.forkeat.service.model.wallet.Wallet;
import fr.uge.forkeat.service.persistence.*;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.port.EventPublisherPort;
import fr.uge.forkeat.service.port.StoragePort;
import fr.uge.forkeat.service.port.UserIdentityPort;
import fr.uge.forkeat.service.security.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipePersistence recipePersistence;
    @Mock
    private WalletPersistence walletPersistence;
    @Mock
    private SuperLikeConfigPersistence superLikeConfigPersistence;
    @Mock
    private PromotionPersistence promotionPersistence;
    @Mock
    private StoragePort storageService;
    @Mock
    private EventPublisherPort<RecipePublishedEvent> eventPublisherPort;
    @Mock
    private AuthenticationPort authPort;
    @Mock
    private UserIdentityPort userIdentityPort;
    @Mock
    private PlatformWalletPersistence platformWalletPersistence;
    @Mock
    private SecurityService securityService;

    private RecipeService recipeService;
    private Instant now;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(recipePersistence, storageService, walletPersistence,
                authPort, superLikeConfigPersistence, promotionPersistence, platformWalletPersistence,
                eventPublisherPort, userIdentityPort, securityService);
        now = Instant.now();
    }

    @Nested
    class FindById {

        @Test
        void shouldReturnRecipeWhenFound() {
            var recipeId = UUID.randomUUID();
            var recipe = createRecipe(recipeId, "Tarte aux pommes", RecipeStatus.PUBLISHED);
            when(recipePersistence.findById(recipeId)).thenReturn(Optional.of(recipe));

            var result = recipeService.findById(recipeId);

            assertNotNull(result);
            assertEquals(recipeId, result.id());
            assertEquals("Tarte aux pommes", result.title());
            verify(recipePersistence).findById(recipeId);
        }

        @Test
        void shouldThrowRecipeNotFoundExceptionWhenNotFound() {
            var recipeId = UUID.randomUUID();
            when(recipePersistence.findById(recipeId)).thenReturn(Optional.empty());

            var exception = assertThrows(RecipeNotFoundException.class,
                    () -> recipeService.findById(recipeId));

            assertEquals(recipeId, exception.getRecipeId());
            verify(recipePersistence).findById(recipeId);
        }
    }

    @Nested
    class FindPersonalizedRecipeById {

        @Test
        void shouldReturnPersonalizedRecipeWhenFound() {
            var recipeId = UUID.randomUUID();
            var recipe = createRecipe(recipeId, "Tarte aux pommes", RecipeStatus.PUBLISHED);
            var counts = new RecipeCounts(10L, 5L, 2L);
            var interaction = new RecipeUserInteraction(true, false, false);
            var username = "user1";

            when(recipePersistence.findById(recipeId)).thenReturn(Optional.of(recipe));
            when(recipePersistence.findRecipeCounts(recipeId)).thenReturn(counts);
            when(recipePersistence.findUserRecipeInteraction(recipeId, username)).thenReturn(interaction);

            var result = recipeService.findPersonalizedRecipeById(recipeId, username);

            assertNotNull(result);
            assertEquals(recipe, result.recipe());
            assertEquals(counts, result.counts());
            assertEquals(interaction, result.interaction());
            verify(recipePersistence).findById(recipeId);
            verify(recipePersistence).findRecipeCounts(recipeId);
            verify(recipePersistence).findUserRecipeInteraction(recipeId, username);
        }

        @Test
        void shouldReturnPersonalizedRecipeWithNoneInteractionWhenUsernameIsNull() {
            var recipeId = UUID.randomUUID();
            var recipe = createRecipe(recipeId, "Tarte aux pommes", RecipeStatus.PUBLISHED);
            var counts = new RecipeCounts(10L, 5L, 2L);

            when(recipePersistence.findById(recipeId)).thenReturn(Optional.of(recipe));
            when(recipePersistence.findRecipeCounts(recipeId)).thenReturn(counts);

            var result = recipeService.findPersonalizedRecipeById(recipeId, null);

            assertNotNull(result);
            assertEquals(recipe, result.recipe());
            assertEquals(counts, result.counts());
            assertEquals(RecipeUserInteraction.NONE, result.interaction());
            verify(recipePersistence).findById(recipeId);
            verify(recipePersistence).findRecipeCounts(recipeId);
            verify(recipePersistence, never()).findUserRecipeInteraction(any(), any());
        }

        @Test
        void shouldThrowRecipeNotFoundExceptionWhenNotFound() {
            var recipeId = UUID.randomUUID();
            when(recipePersistence.findById(recipeId)).thenReturn(Optional.empty());

            assertThrows(RecipeNotFoundException.class,
                    () -> recipeService.findPersonalizedRecipeById(recipeId, "user1"));
        }
    }

    @Nested
    class FindByStatus {

        @Test
        void shouldReturnRecipesWithMatchingStatus() {
            var recipe1 = createRecipe(UUID.randomUUID(), "Recette 1", RecipeStatus.PUBLISHED);
            var recipe2 = createRecipe(UUID.randomUUID(), "Recette 2", RecipeStatus.PUBLISHED);
            when(recipePersistence.findByStatus(RecipeStatus.PUBLISHED)).thenReturn(List.of(recipe1, recipe2));

            var result = recipeService.findByStatus(RecipeStatus.PUBLISHED);

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(r -> r.status() == RecipeStatus.PUBLISHED));
            verify(recipePersistence).findByStatus(RecipeStatus.PUBLISHED);
        }

        @Test
        void shouldReturnEmptyListWhenNoRecipesMatch() {
            when(recipePersistence.findByStatus(RecipeStatus.DRAFT)).thenReturn(List.of());

            var result = recipeService.findByStatus(RecipeStatus.DRAFT);

            assertTrue(result.isEmpty());
            verify(recipePersistence).findByStatus(RecipeStatus.DRAFT);
        }

        @Test
        void shouldReturnDraftRecipes() {
            var draftRecipe = createRecipe(UUID.randomUUID(), "Brouillon", RecipeStatus.DRAFT);
            when(recipePersistence.findByStatus(RecipeStatus.DRAFT)).thenReturn(List.of(draftRecipe));

            var result = recipeService.findByStatus(RecipeStatus.DRAFT);

            assertEquals(1, result.size());
            assertEquals(RecipeStatus.DRAFT, result.getFirst().status());
        }
    }

    @Nested
    class GetRecipesToModerate {
        @Test
        void shouldReturnRecipesToModerate() {
            var moderatorId = UUID.randomUUID();
            var moderatorName = "moderator";
            var moderator = new UserEntity();
            moderator.setId(moderatorId);
            moderator.setUsername(moderatorName);
            var recipe1 = createRecipe(UUID.randomUUID(), "Recette 1", RecipeStatus.PENDING_REVIEW);
            var recipe2 = createRecipe(UUID.randomUUID(), "Recette 2", RecipeStatus.PENDING_REVIEW);
            var page = new PageResult<>(List.of(recipe1, recipe2), 2L);
            when(recipePersistence.getRecipesToModerate(moderatorId, 10, 0)).thenReturn(page);
            when(userIdentityPort.findIdByUsernameOrThrow(moderatorName)).thenReturn(moderatorId);

            var result = recipeService.getRecipesToModerate(moderatorName, 10, 0);

            assertEquals(2, result.items().size());
            assertEquals(2L, result.total());
            assertTrue(result.items().stream().allMatch(r -> r.status() == RecipeStatus.PENDING_REVIEW));
            verify(recipePersistence).getRecipesToModerate(moderatorId, 10, 0);
        }

        @Test
        void shouldReturnEmptyIfNoRecipesToModerate() {
            var moderatorId = UUID.randomUUID();
            var moderatorName = "moderator";
            var moderator = new UserEntity();
            moderator.setId(moderatorId);
            moderator.setUsername(moderatorName);
            var page = new PageResult<Recipe>(List.of(), 0L);
            when(recipePersistence.getRecipesToModerate(moderatorId, 10, 0)).thenReturn(page);
            when(userIdentityPort.findIdByUsernameOrThrow(moderatorName)).thenReturn(moderatorId);

            var result = recipeService.getRecipesToModerate(moderatorName, 10, 0);

            assertTrue(result.items().isEmpty());
            assertEquals(0L, result.total());
        }

        @Test
        void shouldThrowWhenModeratorNameIsUnknown() {
            var moderator = "mod";
            when(userIdentityPort.findIdByUsernameOrThrow(moderator))
                    .thenThrow(new ResourceNotFoundException("User not found: " + moderator));
            assertThrows(ResourceNotFoundException.class, () -> recipeService.getRecipesToModerate(moderator, 10, 0));
        }

        @Test
        void shouldThrowWhenModeratorIsNull() {
            assertThrows(NullPointerException.class, () -> recipeService.getRecipesToModerate(null, 10, 0));
        }

        @Test
        void shouldThrowWhenPageOrSizeInvalid() {
            assertThrows(IllegalArgumentException.class, () -> recipeService.getRecipesToModerate("moderator1", 0, 0));
            assertThrows(IllegalArgumentException.class, () -> recipeService.getRecipesToModerate("moderator1", 10, -1));
        }
    }

    @Nested
    class SearchRecipes {

        @Test
        void shouldReturnPersonalizedSummariesWhenAuthenticated() {
            var recipeId = UUID.randomUUID();
            var summary = createRecipeSummary(recipeId, "Tarte");
            var counts = new RecipeCounts(5L, 2L, 1L);
            var interaction = new RecipeUserInteraction(true, false, false);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, "tarte", List.of(), 12, 0);

            when(authPort.extractUsername()).thenReturn("user1");
            when(recipePersistence.searchRecipes(criteria)).thenReturn(new PageResult<>(List.of(summary), 1L));
            when(recipePersistence.findRecipeCounts(List.of(recipeId))).thenReturn(Map.of(recipeId, counts));
            when(recipePersistence.findUserRecipeInteractions(List.of(recipeId), "user1")).thenReturn(Map.of(recipeId, interaction));

            var result = recipeService.searchRecipes(criteria);

            assertEquals(1, result.items().size());
            var item = result.items().getFirst();
            assertEquals("Tarte", item.summary().title());
            assertEquals(5L, item.counts().likeCount());
            assertEquals(2L, item.counts().superLikeCount());
            assertTrue(item.interaction().likedByCurrentUser());
            verify(recipePersistence).searchRecipes(criteria);
            verify(recipePersistence).findRecipeCounts(List.of(recipeId));
            verify(recipePersistence).findUserRecipeInteractions(List.of(recipeId), "user1");
        }

        @Test
        void shouldReturnNoneInteractionWhenNotAuthenticated() {
            var recipeId = UUID.randomUUID();
            var summary = createRecipeSummary(recipeId, "Salade");
            var counts = new RecipeCounts(3L, 0L, 0L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, "salade", List.of(), 12, 0);

            when(authPort.extractUsername()).thenReturn(null);
            when(recipePersistence.searchRecipes(criteria)).thenReturn(new PageResult<>(List.of(summary), 1L));
            when(recipePersistence.findRecipeCounts(List.of(recipeId))).thenReturn(Map.of(recipeId, counts));

            var result = recipeService.searchRecipes(criteria);

            assertEquals(1, result.items().size());
            var item = result.items().getFirst();
            assertEquals(RecipeUserInteraction.NONE, item.interaction());
            verify(recipePersistence, never()).findUserRecipeInteractions(any(), any());
        }

        @Test
        void shouldReturnEmptyPageWhenNoResults() {
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, "inexistant", List.of(), 12, 0);

            when(authPort.extractUsername()).thenReturn("user1");
            when(recipePersistence.searchRecipes(criteria)).thenReturn(new PageResult<>(List.of(), 0L));

            var result = recipeService.searchRecipes(criteria);

            assertTrue(result.items().isEmpty());
            assertEquals(0L, result.total());
            verify(recipePersistence, never()).findRecipeCounts(Collections.singletonList(any()));
            verify(recipePersistence, never()).findUserRecipeInteractions(any(), any());
        }

        @Test
        void shouldThrowWhenCriteriaIsNull() {
            assertThrows(NullPointerException.class,
                    () -> recipeService.searchRecipes(null));
        }
    }

    @Nested
    class CreateRecipe {

        @Test
        void shouldSaveRecipeWithoutImage() {
            var recipeId = UUID.randomUUID();
            var recipe = createRecipe(recipeId, "Tarte aux pommes", RecipeStatus.DRAFT);
            when(recipePersistence.save(any(Recipe.class))).thenReturn(recipe);

            var result = recipeService.createRecipe(recipe, null);

            assertNotNull(result);
            assertEquals(recipeId, result.id());
            verify(storageService, never()).uploadImage(any(), any());
            verify(recipePersistence).save(any(Recipe.class));
        }

        @Test
        void shouldSaveRecipeWithImage() {
            var recipeId = UUID.randomUUID();
            var recipe = createRecipe(recipeId, "Quiche Lorraine", RecipeStatus.DRAFT);
            var newImage = new ImageUpload(new byte[]{1, 2, 3}, "image/jpeg", "quiche.jpg");
            when(storageService.uploadImage(newImage, "recipes")).thenReturn("https://cloudflare.com/recipes/maxtekpa.jpg");
            var expectedRecipe = new Recipe(
                    recipeId, "Quiche Lorraine", "Summary for Quiche Lorraine", null,
                    "chef_test", 30, "https://cloudflare.com/recipes/maxtekpa.jpg",
                    RecipeStatus.DRAFT, List.of(), List.of(), List.of(), List.of(), now, now
            );
            when(recipePersistence.save(any(Recipe.class))).thenReturn(expectedRecipe);

            var result = recipeService.createRecipe(recipe, newImage);

            assertNotNull(result);
            assertEquals("https://cloudflare.com/recipes/maxtekpa.jpg", result.imageUrl());
            verify(storageService).uploadImage(newImage, "recipes");
            verify(recipePersistence).save(any(Recipe.class));
        }
    }

    @Nested
    class UpdateRecipe {
        @Test
        void shouldUpdateRecipeWithoutImage() {
            var recipeId = UUID.randomUUID();
            var existingRecipe = createRecipe(recipeId, "Old Title", RecipeStatus.DRAFT);
            var updatedRecipe = createRecipe(recipeId, "New Title", RecipeStatus.PUBLISHED);

            //when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipePersistence.findById(recipeId)).thenReturn(Optional.of(existingRecipe));
            when(recipePersistence.update(eq(recipeId), any(Recipe.class))).thenReturn(updatedRecipe);
            when(securityService.canUpdateRecipe(any())).thenReturn(true);

            var result = recipeService.updateRecipe(recipeId, updatedRecipe, null);

            assertNotNull(result);
            assertEquals("New Title", result.title());
            verify(storageService, never()).uploadImage(any(), any());
            verify(recipePersistence).update(eq(recipeId), any(Recipe.class));
        }

        @Test
        void shouldUpdateRecipeWithNewImageAndRemoveOldOne() {
            var recipeId = UUID.randomUUID();
            var existingRecipe = new Recipe(
                    recipeId, "Old Title", "Summary", null, "chef_test", 30,
                    "https://old.image.url", RecipeStatus.DRAFT, List.of(), List.of(), List.of(), List.of(), now, now
            );
            var updatedRecipe = createRecipe(recipeId, "New Title", RecipeStatus.PUBLISHED);
            var newImage = new ImageUpload(new byte[]{1, 2, 3}, "image/jpeg", "new.jpg");

            //when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipePersistence.findById(recipeId)).thenReturn(Optional.of(existingRecipe));
            when(storageService.uploadImage(newImage, "recipes")).thenReturn("https://new.image.url");
            when(recipePersistence.update(eq(recipeId), any(Recipe.class))).thenReturn(updatedRecipe);
            when(securityService.canUpdateRecipe(any())).thenReturn(true);

            recipeService.updateRecipe(recipeId, updatedRecipe, newImage);

            verify(storageService).deleteImage("https://old.image.url");
            verify(storageService).uploadImage(newImage, "recipes");
            verify(recipePersistence).update(eq(recipeId), any(Recipe.class));
        }
    }

    @Nested
    class UpdateRecipeByStatus {
        @Test
        void shouldUpdateRecipeStatus() {
            var recipeId = UUID.randomUUID();
            var recipe = createRecipe(recipeId, "test", RecipeStatus.PUBLISHED);
            when(recipePersistence.updateStatus(recipeId, RecipeStatus.PUBLISHED)).thenReturn(recipe);

            assertEquals(RecipeStatus.PUBLISHED, recipeService.updateStatus(recipeId, RecipeStatus.PUBLISHED).status());
        }

        @Test
        void shouldThrowWhenRecipeNotFound() {
            var recipeId = UUID.randomUUID();
            when(recipePersistence.updateStatus(recipeId, RecipeStatus.PUBLISHED)).thenThrow(RecipeNotFoundException.class);

            assertThrows(RecipeNotFoundException.class, () -> recipeService.updateStatus(recipeId, RecipeStatus.PUBLISHED));
        }
    }

    @Nested
    class DeleteById {

        @Test
        void shouldDeleteRecipeWithoutImage() {
            var recipeId = UUID.randomUUID();
            var recipe = createRecipe(recipeId, "Tarte", RecipeStatus.PUBLISHED);
            //when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipePersistence.findById(recipeId)).thenReturn(Optional.of(recipe));
            when(securityService.canDeleteRecipe(any())).thenReturn(true);

            recipeService.deleteById(recipeId);

            verify(storageService, never()).deleteImage(any());
            verify(recipePersistence).deleteById(recipeId);
        }

        @Test
        void shouldDeleteRecipeAndImage() {
            var recipeId = UUID.randomUUID();
            var recipe = new Recipe(
                    recipeId, "Quiche", "Summary", null, "chef_test", 30,
                    "https://cdn.example.com/recipes/img.jpg", RecipeStatus.PUBLISHED,
                    List.of(), List.of(), List.of(), List.of(), now, now
            );
          //  when(authPort.extractUsername()).thenReturn("chef_test");
            when(recipePersistence.findById(recipeId)).thenReturn(Optional.of(recipe));
            when(securityService.canDeleteRecipe(any())).thenReturn(true);

            recipeService.deleteById(recipeId);

            verify(storageService).deleteImage("https://cdn.example.com/recipes/img.jpg");
            verify(recipePersistence).deleteById(recipeId);
        }

        @Test
        void shouldThrowWhenRecipeNotFound() {
            var recipeId = UUID.randomUUID();
            when(recipePersistence.findById(recipeId)).thenReturn(Optional.empty());

            assertThrows(RecipeNotFoundException.class, () -> recipeService.deleteById(recipeId));

            verify(recipePersistence, never()).deleteById(any());
            verify(storageService, never()).deleteImage(any());
        }
    }

    @Nested
    class FindAllAllergens {

        @Test
        void shouldDelegateToPersistence() {
            var allergens = List.of(
                    new Allergen(UUID.randomUUID(), "Gluten", AllergenSeverity.HIGH),
                    new Allergen(UUID.randomUUID(), "Lactose", AllergenSeverity.MEDIUM)
            );
            when(recipePersistence.findAllAllergens()).thenReturn(allergens);

            var result = recipeService.findAllAllergens();

            assertEquals(2, result.size());
            assertEquals("Gluten", result.getFirst().name());
            verify(recipePersistence).findAllAllergens();
        }

        @Test
        void shouldReturnEmptyListWhenNone() {
            when(recipePersistence.findAllAllergens()).thenReturn(List.of());

            var result = recipeService.findAllAllergens();

            assertTrue(result.isEmpty());
            verify(recipePersistence).findAllAllergens();
        }
    }

    @Nested
    class FindAllDietaryNames {

        @Test
        void shouldDelegateToPersistence() {
            when(recipePersistence.findAllDietaryNames()).thenReturn(List.of("halal", "vegan", "végétarien"));

            var result = recipeService.findAllDietaryNames();

            assertEquals(3, result.size());
            assertEquals("halal", result.get(0));
            verify(recipePersistence).findAllDietaryNames();
        }

        @Test
        void shouldReturnEmptyListWhenNone() {
            when(recipePersistence.findAllDietaryNames()).thenReturn(List.of());

            var result = recipeService.findAllDietaryNames();

            assertTrue(result.isEmpty());
            verify(recipePersistence).findAllDietaryNames();
        }
    }

    @Nested
    class LikeUnlikeRecipe {
        @Test
        void shouldLikeRecipe() {
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();

            when(recipePersistence.existRecipe(recipeId)).thenReturn(true);

            recipeService.likeRecipe(userId, recipeId);

            verify(recipePersistence).likeRecipe(userId, recipeId);
        }

        @Test
        void shouldThrowWhenLikingNonExistentRecipe() {
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();

            when(recipePersistence.existRecipe(recipeId)).thenReturn(false);

            assertThrows(RecipeNotFoundException.class, () -> recipeService.likeRecipe(userId, recipeId));
            verify(recipePersistence, never()).likeRecipe(any(), any());
        }

        @Test
        void shouldUnlikeRecipe() {
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();

            when(recipePersistence.existRecipe(recipeId)).thenReturn(true);

            recipeService.unlikeRecipe(userId, recipeId);

            verify(recipePersistence).unlikeRecipe(userId, recipeId);
        }

        @Test
        void shouldThrowWhenUnlikingNonExistentRecipe() {
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();

            when(recipePersistence.existRecipe(recipeId)).thenReturn(false);

            assertThrows(RecipeNotFoundException.class, () -> recipeService.unlikeRecipe(userId, recipeId));
            verify(recipePersistence, never()).unlikeRecipe(any(), any());
        }
    }

    private RecipeSummary createRecipeSummary(UUID id, String title) {
        return new RecipeSummary(id, title, "Summary for " + title, null, 30, now, "chef_test");
    }

    @Nested
    class SuperLikeRecipe {

        @Test
        void SuperLikeShouldBeOk() {
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            var config = new SuperLikeConfig(UUID.randomUUID(), 100L, new BigDecimal("0.40"), Instant.now());

            when(recipePersistence.hasSuperLikedRecipe(userId, recipeId)).thenReturn(false);
            when(walletPersistence.saveTransaction(any())).thenReturn(null);
            doNothing().when(recipePersistence).superLikeRecipe(any(), any(), anyLong(), any(), anyBoolean());
            doNothing().when(walletPersistence).incrementBalanceById(any(), anyLong());
            when(walletPersistence.loadWalletWithLock(userId)).thenReturn(Optional.of(createWallet(200L)));
            when(superLikeConfigPersistence.get()).thenReturn(config);
            when(promotionPersistence.findActiveAt(any())).thenReturn(Optional.empty());
            when(walletPersistence.getEarningsWallet()).thenReturn(createWallet(0L));
            when(walletPersistence.getRedistributionWallet()).thenReturn(createWallet(0L));

            recipeService.superLikeRecipe(userId, recipeId);

            verify(recipePersistence).superLikeRecipe(eq(userId), eq(recipeId), eq(100L), isNull(), eq(false));
        }

        @Test
        void ShouldNotSuperLikeWhenItAlreadySuperLiked() {
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();

            when(recipePersistence.hasSuperLikedRecipe(any(), any())).thenReturn(true);

            recipeService.superLikeRecipe(userId, recipeId);

            verify(recipePersistence, never()).superLikeRecipe(any(), any(), anyLong(), any(), anyBoolean());
            verify(walletPersistence, never()).loadWalletWithLock(any());
            verify(walletPersistence, never()).incrementBalanceById(any(), anyLong());
            verify(walletPersistence, never()).getRedistributionWallet();
            verify(walletPersistence, never()).getEarningsWallet();
            verify(recipePersistence, times(1)).hasSuperLikedRecipe(any(), any());
        }

        @Test
        void shouldThrowInsufficientFundsExceptionWhenBalanceTooLow() {
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            var config = new SuperLikeConfig(UUID.randomUUID(), 100L, new BigDecimal("0.40"), Instant.now());

            when(recipePersistence.hasSuperLikedRecipe(userId, recipeId)).thenReturn(false);
            when(walletPersistence.loadWalletWithLock(userId)).thenReturn(Optional.of(createWallet(50L)));
            when(superLikeConfigPersistence.get()).thenReturn(config);
            when(promotionPersistence.findActiveAt(any())).thenReturn(Optional.empty());

            assertThrows(InsufficientFundsException.class,
                    () -> recipeService.superLikeRecipe(userId, recipeId));

            verify(recipePersistence, never()).superLikeRecipe(any(), any(), anyLong(), any(), anyBoolean());
            verify(walletPersistence, never()).incrementBalanceById(any(), anyLong());
            verify(walletPersistence, never()).getEarningsWallet();
            verify(walletPersistence, never()).getRedistributionWallet();
        }
    }

    @Nested
    class FindRecipesByAuthor {

        private final UUID authorId = UUID.randomUUID();

        @Test
        void shouldReturnAuthorRecipesPage() {
            var summary = createRecipeSummary(UUID.randomUUID(), "Ma recette");
            var authorSummary = new AuthorRecipeSummary(summary, RecipeStatus.PUBLISHED, null);
            var stats = new UserRecipeStats(1, 0, 0, 0);
            var page = new PageResult<>(List.of(authorSummary), 1L);

            when(userIdentityPort.findIdByUsernameOrThrow("chef_test")).thenReturn(authorId);
            when(recipePersistence.countRecipesByAuthorGroupedByStatus(authorId)).thenReturn(stats);
            when(recipePersistence.findRecipesByAuthor(authorId, RecipeStatus.PUBLISHED, 0, 10)).thenReturn(page);

            var result = recipeService.findRecipesByAuthor("chef_test", RecipeStatus.PUBLISHED, 0, 10);

            assertNotNull(result);
            assertEquals(1, result.recipes().items().size());
            assertEquals(1L, result.recipes().total());
            assertEquals(stats, result.stats());
        }

        @Test
        void shouldResolveUsernameToAuthorId() {
            when(userIdentityPort.findIdByUsernameOrThrow("chef_test")).thenReturn(authorId);
            when(recipePersistence.countRecipesByAuthorGroupedByStatus(authorId)).thenReturn(UserRecipeStats.ZERO);
            when(recipePersistence.findRecipesByAuthor(authorId, RecipeStatus.PUBLISHED, 0, 10)).thenReturn(new PageResult<>(List.of(), 0L));

            recipeService.findRecipesByAuthor("chef_test", RecipeStatus.PUBLISHED, 0, 10);

            verify(userIdentityPort).findIdByUsernameOrThrow("chef_test");
            verify(recipePersistence).countRecipesByAuthorGroupedByStatus(authorId);
            verify(recipePersistence).findRecipesByAuthor(authorId, RecipeStatus.PUBLISHED, 0, 10);
        }

        @Test
        void shouldForwardStatusPageAndSize() {
            when(userIdentityPort.findIdByUsernameOrThrow("chef_test")).thenReturn(authorId);
            when(recipePersistence.countRecipesByAuthorGroupedByStatus(authorId)).thenReturn(UserRecipeStats.ZERO);
            when(recipePersistence.findRecipesByAuthor(authorId, RecipeStatus.DRAFT, 2, 5)).thenReturn(new PageResult<>(List.of(), 0L));

            recipeService.findRecipesByAuthor("chef_test", RecipeStatus.DRAFT, 2, 5);

            verify(recipePersistence).findRecipesByAuthor(authorId, RecipeStatus.DRAFT, 2, 5);
        }

        @Test
        void shouldThrowWhenUsernameIsNull() {
            assertThrows(NullPointerException.class,
                    () -> recipeService.findRecipesByAuthor(null, RecipeStatus.PUBLISHED, 0, 10));
            verifyNoInteractions(recipePersistence);
        }

        @Test
        void shouldThrowWhenStatusIsNull() {
            assertThrows(NullPointerException.class,
                    () -> recipeService.findRecipesByAuthor("chef_test", null, 0, 10));
            verifyNoInteractions(recipePersistence);
        }

        @Test
        void shouldThrowWhenSizeIsZeroOrNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> recipeService.findRecipesByAuthor("chef_test", RecipeStatus.PUBLISHED, 0, 0));
            assertThrows(IllegalArgumentException.class,
                    () -> recipeService.findRecipesByAuthor("chef_test", RecipeStatus.PUBLISHED, 0, -1));
            verifyNoInteractions(recipePersistence);
        }

        @Test
        void shouldThrowWhenPageIsNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> recipeService.findRecipesByAuthor("chef_test", RecipeStatus.PUBLISHED, -1, 10));
            verifyNoInteractions(recipePersistence);
        }
    }

    @Nested
    class GetPersonalizedFeedRecipesTest {

        private static final Instant NOW = Instant.parse("2024-01-15T10:00:00Z");
        private static final int PAGE = 0;
        private static final String USERNAME = "john.doe";
        private static final UUID RECIPE_ID_1 = UUID.randomUUID();
        private static final UUID RECIPE_ID_2 = UUID.randomUUID();

        @Nested
        class WhenPageIsEmpty {

            @Test
            void shouldReturnEmptyPageResult() {
                // Given
                when(authPort.extractUsername()).thenReturn(USERNAME);
                when(recipePersistence.searchPersonalizedFeedRecipes(USERNAME, NOW, PAGE))
                        .thenReturn(new PageResult<>(List.of(), 0L));

                // When
                var result = recipeService.getPersonalizedFeedRecipes(NOW, PAGE);

                // Then
                assertEquals(0L, result.total());
                assertTrue(result.items().isEmpty());
            }

            @Test
            void shouldNotFetchCountsNorInteractions() {
                // Given
                when(authPort.extractUsername()).thenReturn(USERNAME);
                when(recipePersistence.searchPersonalizedFeedRecipes(USERNAME, NOW, PAGE))
                        .thenReturn(new PageResult<>(List.of(), 0L));

                // When
                recipeService.getPersonalizedFeedRecipes(NOW, PAGE);

                // Then
                verify(recipePersistence, never()).findRecipeCounts((UUID) any());
                verify(recipePersistence, never()).findUserRecipeInteractions(any(), any());
            }
        }

        @Nested
        class WhenUserIsAuthenticated {

            @Test
            void shouldReturnPersonalizedSummariesWithCountsAndInteractions() {
                // Given
                var summary1 = aRecipeSummary(RECIPE_ID_1);
                var summary2 = aRecipeSummary(RECIPE_ID_2);
                var counts1 = new RecipeCounts(10, 5, 0);
                var counts2 = new RecipeCounts(3, 1, 0);
                var interaction1 = new RecipeUserInteraction(true, false, false);
                var interaction2 = new RecipeUserInteraction(false, true, false);

                when(authPort.extractUsername()).thenReturn(USERNAME);
                when(recipePersistence.searchPersonalizedFeedRecipes(USERNAME, NOW, PAGE))
                        .thenReturn(new PageResult<>(List.of(summary1, summary2), 2L));
                when(recipePersistence.findRecipeCounts(List.of(RECIPE_ID_1, RECIPE_ID_2)))
                        .thenReturn(Map.of(RECIPE_ID_1, counts1, RECIPE_ID_2, counts2));
                when(recipePersistence.findUserRecipeInteractions(List.of(RECIPE_ID_1, RECIPE_ID_2), USERNAME))
                        .thenReturn(Map.of(RECIPE_ID_1, interaction1, RECIPE_ID_2, interaction2));

                // When
                var result = recipeService.getPersonalizedFeedRecipes(NOW, PAGE);

                // Then
                assertEquals(2L, result.total());

                var first = result.items().get(0);
                assertEquals(summary1, first.summary());
                assertEquals(counts1, first.counts());
                assertEquals(interaction1, first.interaction());

                var second = result.items().get(1);
                assertEquals(summary2, second.summary());
                assertEquals(counts2, second.counts());
                assertEquals(interaction2, second.interaction());
            }

            @Test
            void shouldFallbackToZeroCountsWhenRecipeNotInCountsMap() {
                // Given
                var summary = aRecipeSummary(RECIPE_ID_1);

                when(authPort.extractUsername()).thenReturn(USERNAME);
                when(recipePersistence.searchPersonalizedFeedRecipes(USERNAME, NOW, PAGE))
                        .thenReturn(new PageResult<>(List.of(summary), 1L));
                when(recipePersistence.findRecipeCounts(List.of(RECIPE_ID_1)))
                        .thenReturn(Map.of());
                when(recipePersistence.findUserRecipeInteractions(List.of(RECIPE_ID_1), USERNAME))
                        .thenReturn(Map.of(RECIPE_ID_1, new RecipeUserInteraction(true, false, false)));

                // When
                var result = recipeService.getPersonalizedFeedRecipes(NOW, PAGE);

                // Then
                assertEquals(RecipeCounts.ZERO, result.items().get(0).counts());
            }

            @Test
            void shouldFallbackToNoneInteractionWhenRecipeNotInInteractionsMap() {
                // Given
                var summary = aRecipeSummary(RECIPE_ID_1);

                when(authPort.extractUsername()).thenReturn(USERNAME);
                when(recipePersistence.searchPersonalizedFeedRecipes(USERNAME, NOW, PAGE))
                        .thenReturn(new PageResult<>(List.of(summary), 1L));
                when(recipePersistence.findRecipeCounts(List.of(RECIPE_ID_1)))
                        .thenReturn(Map.of(RECIPE_ID_1, new RecipeCounts(5, 2, 0)));
                when(recipePersistence.findUserRecipeInteractions(List.of(RECIPE_ID_1), USERNAME))
                        .thenReturn(Map.of());

                // When
                var result = recipeService.getPersonalizedFeedRecipes(NOW, PAGE);

                // Then
                assertEquals(RecipeUserInteraction.NONE, result.items().get(0).interaction());
            }
        }

        @Nested
        class WhenUserIsAnonymous {

            @Test
            void shouldNotFetchUserInteractions() {
                // Given
                var summary = aRecipeSummary(RECIPE_ID_1);

                when(authPort.extractUsername()).thenReturn(null);
                when(recipePersistence.searchPersonalizedFeedRecipes(null, NOW, PAGE))
                        .thenReturn(new PageResult<>(List.of(summary), 1L));
                when(recipePersistence.findRecipeCounts(List.of(RECIPE_ID_1)))
                        .thenReturn(Map.of(RECIPE_ID_1, new RecipeCounts(5, 2, 0)));

                // When
                recipeService.getPersonalizedFeedRecipes(NOW, PAGE);

                // Then
                verify(recipePersistence, never()).findUserRecipeInteractions(any(), any());
            }

            @Test
            void shouldReturnNoneInteractionForAllSummaries() {
                // Given
                var summary = aRecipeSummary(RECIPE_ID_1);

                when(authPort.extractUsername()).thenReturn(null);
                when(recipePersistence.searchPersonalizedFeedRecipes(null, NOW, PAGE))
                        .thenReturn(new PageResult<>(List.of(summary), 1L));
                when(recipePersistence.findRecipeCounts(List.of(RECIPE_ID_1)))
                        .thenReturn(Map.of(RECIPE_ID_1, new RecipeCounts(5, 2, 5)));

                // When
                var result = recipeService.getPersonalizedFeedRecipes(NOW, PAGE);

                // Then
                assertEquals(RecipeUserInteraction.NONE, result.items().get(0).interaction());
            }
        }

        // --- helpers ---

        private RecipeSummary aRecipeSummary(UUID id) {
            return new RecipeSummary(id, "Recipe " + id, "", "", 0, Instant.now(), "");
        }
    }

    private Recipe createRecipe(UUID id, String title, RecipeStatus status) {
        return new Recipe(
                id,
                title,
                "Summary for " + title,
                null,
                "chef_test",
                30,
                null,
                status,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                now,
                now
        );
    }

    private Wallet createWallet() {
        return createWallet(1000L);
    }

    private Wallet createWallet(long balance) {
        return new Wallet(
                UUID.randomUUID(),
                UUID.randomUUID(),
                balance,
                Instant.now()
        );
    }
}
