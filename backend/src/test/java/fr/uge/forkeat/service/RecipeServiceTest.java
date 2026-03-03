package fr.uge.forkeat.service;

import fr.uge.forkeat.service.event.RecipePublishedEvent;
import fr.uge.forkeat.service.exception.InsufficientFundsException;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.ImageUpload;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.recipe.projection.*;
import fr.uge.forkeat.service.model.wallet.Wallet;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import fr.uge.forkeat.service.port.EventPublisherPort;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.port.StoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private StoragePort storageService;
    @Mock
    private EventPublisherPort<RecipePublishedEvent> eventPublisherPort;
    @Mock
    private AuthenticationPort authPort;


    private RecipeService recipeService;
    private Instant now;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(recipePersistence, storageService, walletPersistence, authPort, eventPublisherPort);
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
            var counts = new RecipeCounts(10L, 5L);
            var interaction = new RecipeUserInteraction(true, false);
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
            var counts = new RecipeCounts(10L, 5L);

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
    class SearchRecipes {

        @Test
        void shouldReturnPersonalizedSummariesWhenAuthenticated() {
            var recipeId = UUID.randomUUID();
            var summary = createRecipeSummary(recipeId, "Tarte");
            var counts = new RecipeCounts(5L, 2L);
            var interaction = new RecipeUserInteraction(true, false);
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
            var counts = new RecipeCounts(3L, 0L);
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

            when(recipePersistence.findById(recipeId)).thenReturn(Optional.of(existingRecipe));
            when(recipePersistence.update(eq(recipeId), any(Recipe.class))).thenReturn(updatedRecipe);

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

            when(recipePersistence.findById(recipeId)).thenReturn(Optional.of(existingRecipe));
            when(storageService.uploadImage(newImage, "recipes")).thenReturn("https://new.image.url");
            when(recipePersistence.update(eq(recipeId), any(Recipe.class))).thenReturn(updatedRecipe);

            recipeService.updateRecipe(recipeId, updatedRecipe, newImage);

            verify(storageService).deleteImage("https://old.image.url");
            verify(storageService).uploadImage(newImage, "recipes");
            verify(recipePersistence).update(eq(recipeId), any(Recipe.class));
        }
    }

    @Nested
    class DeleteById {

        @Test
        void shouldDeleteRecipeWithoutImage() {
            var recipeId = UUID.randomUUID();
            var recipe = createRecipe(recipeId, "Tarte", RecipeStatus.PUBLISHED);
            when(recipePersistence.findById(recipeId)).thenReturn(Optional.of(recipe));

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
            when(recipePersistence.findById(recipeId)).thenReturn(Optional.of(recipe));

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
    class SuperLikeRecipe{
        @Test
        void SuperLikeShouldBeOk(){
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();

            when(recipePersistence.hasSuperLikedRecipe(userId, recipeId)).thenReturn(false);
            when(walletPersistence.getBalance(userId)).thenReturn(200L);
            when(walletPersistence.getEarningsWallet()).thenReturn(createWallet());
            when(walletPersistence.getRedistributionWallet()).thenReturn(createWallet());
            doNothing().when(recipePersistence).superLikeRecipe(any(), any(), anyLong());
            doNothing().when(walletPersistence).incrementBalanceById(any(), anyLong());

            recipeService.superLikeRecipe(userId, recipeId);

            verify(recipePersistence).superLikeRecipe(eq(userId), eq(recipeId), anyLong());
        }

        @Test
        void ShouldNotSuperLikeWhenItAlreadySuperLiked(){
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();

            when(recipePersistence.hasSuperLikedRecipe(any(), any())).thenReturn(true);

            recipeService.superLikeRecipe(userId, recipeId);

            verify(recipePersistence, never()).superLikeRecipe(any(), any(), anyLong());
            verify(walletPersistence, never()).incrementBalanceById(any(), anyLong());
            verify(walletPersistence, never()).getRedistributionWallet();
            verify(walletPersistence, never()).getEarningsWallet();
            verify(recipePersistence, times(1)).hasSuperLikedRecipe(any(), any());
        }

        @Test
        void shouldThrowInsufficientFundsExceptionWhenBalanceTooLow(){
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();

            when(recipePersistence.hasSuperLikedRecipe(userId, recipeId)).thenReturn(false);
            when(walletPersistence.getBalance(userId)).thenReturn(50L);

            assertThrows(InsufficientFundsException.class,
                    () -> recipeService.superLikeRecipe(userId, recipeId));

            verify(recipePersistence, never()).superLikeRecipe(any(), any(), anyLong());
            verify(walletPersistence, never()).incrementBalanceById(any(), anyLong());
            verify(walletPersistence, never()).getEarningsWallet();
            verify(walletPersistence, never()).getRedistributionWallet();
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
        return new Wallet(
                UUID.randomUUID(),
                UUID.randomUUID(),
                0,
                Instant.now()
        );
    }
}