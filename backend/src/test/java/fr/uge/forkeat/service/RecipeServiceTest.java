package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.ImageUpload;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipe;
import fr.uge.forkeat.service.model.recipe.projection.RecipeCounts;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import fr.uge.forkeat.service.port.StoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipePersistence recipePersistence;
    @Mock
    private StoragePort storageService;

    private RecipeService recipeService;
    private Instant now;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(recipePersistence, storageService);
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
        void shouldDelegateToPersistence() {
            var recipe = createRecipe(UUID.randomUUID(), "Tarte", RecipeStatus.PUBLISHED);
            var pageResult = new PageResult<>(List.of(recipe), 1L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, "tarte", List.of(), 12, 0);
            when(recipePersistence.searchRecipes(criteria)).thenReturn(pageResult);

            var result = recipeService.searchRecipes(criteria);

            assertEquals(1, result.items().size());
            assertEquals("Tarte", result.items().getFirst().title());
            verify(recipePersistence).searchRecipes(criteria);
        }

        @Test
        void shouldThrowWhenCriteriaIsNull() {
            assertThrows(NullPointerException.class,
                    () -> recipeService.searchRecipes(null));
        }

        @Test
        void shouldWorkWithAllergens() {
            var recipe = createRecipe(UUID.randomUUID(), "Salade", RecipeStatus.PUBLISHED);
            var pageResult = new PageResult<>(List.of(recipe), 1L);
            var allergens = List.of("Gluten", "Lactose");
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, "salade", allergens, 12, 0);
            when(recipePersistence.searchRecipes(criteria)).thenReturn(pageResult);

            var result = recipeService.searchRecipes(criteria);

            assertEquals(1, result.items().size());
            verify(recipePersistence).searchRecipes(criteria);
        }

        @Test
        void shouldWorkWithNullSearch() {
            var recipe = createRecipe(UUID.randomUUID(), "Recette", RecipeStatus.PUBLISHED);
            var pageResult = new PageResult<>(List.of(recipe), 1L);
            var criteria = new RecipeSearchCriteria(RecipeStatus.PUBLISHED, null, List.of(), 12, 0);
            when(recipePersistence.searchRecipes(criteria)).thenReturn(pageResult);

            var result = recipeService.searchRecipes(criteria);

            assertEquals(1, result.items().size());
            verify(recipePersistence).searchRecipes(criteria);
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
                    RecipeStatus.DRAFT, List.of(), List.of(), List.of(), Map.of(), now, now
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
                    "https://old.image.url", RecipeStatus.DRAFT, List.of(), List.of(), List.of(), Map.of(), now, now
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
    class UpdateRecipeByStatus {
        @Test
        void shouldUpdateRecipeStatus() {
            var recipeId = UUID.randomUUID();
            when(recipePersistence.updateByStatus(recipeId, RecipeStatus.PUBLISHED)).thenReturn(RecipeStatus.PUBLISHED);

            assertEquals(RecipeStatus.PUBLISHED, recipeService.updateRecipeByStatus(recipeId, RecipeStatus.PUBLISHED));
        }

        @Test
        void shouldThrowWhenRecipeNotFound() {
            var recipeId = UUID.randomUUID();
            when(recipePersistence.updateByStatus(recipeId, RecipeStatus.PUBLISHED)).thenThrow(RecipeNotFoundException.class);

            assertThrows(RecipeNotFoundException.class, () -> recipeService.updateRecipeByStatus(recipeId, RecipeStatus.PUBLISHED));
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
                    List.of(), List.of(), List.of(), Map.of(), now, now
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
    class LikeUnlikeRecipe {
        @Test
        void shouldLikeRecipe() {
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            var recipe = createRecipe(recipeId, "Tarte", RecipeStatus.PUBLISHED);

            when(recipePersistence.findById(recipeId)).thenReturn(Optional.of(recipe));

            recipeService.likeRecipe(userId, recipeId);

            verify(recipePersistence).likeRecipe(userId, recipeId);
        }

        @Test
        void shouldThrowWhenLikingNonExistentRecipe() {
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();

            when(recipePersistence.findById(recipeId)).thenReturn(Optional.empty());

            assertThrows(RecipeNotFoundException.class, () -> recipeService.likeRecipe(userId, recipeId));
            verify(recipePersistence, never()).likeRecipe(any(), any());
        }

        @Test
        void shouldUnlikeRecipe() {
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();
            var recipe = createRecipe(recipeId, "Tarte", RecipeStatus.PUBLISHED);

            when(recipePersistence.findById(recipeId)).thenReturn(Optional.of(recipe));

            recipeService.unlikeRecipe(userId, recipeId);

            verify(recipePersistence).unlikeRecipe(userId, recipeId);
        }

        @Test
        void shouldThrowWhenUnlikingNonExistentRecipe() {
            var userId = UUID.randomUUID();
            var recipeId = UUID.randomUUID();

            when(recipePersistence.findById(recipeId)).thenReturn(Optional.empty());

            assertThrows(RecipeNotFoundException.class, () -> recipeService.unlikeRecipe(userId, recipeId));
            verify(recipePersistence, never()).unlikeRecipe(any(), any());
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
                Map.of(),
                now,
                now
        );
    }
}