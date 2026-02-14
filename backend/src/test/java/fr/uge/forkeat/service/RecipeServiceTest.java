package fr.uge.forkeat.service;

import fr.uge.forkeat.infrastructure.storage.R2StorageService;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Allergen;
import fr.uge.forkeat.service.model.recipe.AllergenSeverity;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeSearchCriteria;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.persistence.RecipePersistence;
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

import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipePersistence recipePersistence;
    @Mock
    private R2StorageService storageService;

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
            var image = mock(MultipartFile.class);
            when(image.isEmpty()).thenReturn(false);
            when(storageService.uploadImage(image, "recipes")).thenReturn("https://cloudflare.com/recipes/maxtekpa.jpg");
            var expectedRecipe = new Recipe(
                    recipeId, "Quiche Lorraine", "Summary for Quiche Lorraine", null,
                    "chef_test", 30, "https://cloudflare.com/recipes/pidali.jpg",
                    RecipeStatus.DRAFT, List.of(), List.of(), List.of(), Map.of(), now, now
            );
            when(recipePersistence.save(any(Recipe.class))).thenReturn(expectedRecipe);

            var result = recipeService.createRecipe(recipe, image);

            assertNotNull(result);
            assertEquals("https://cloudflare.com/recipes/pidali.jpg", result.imageUrl());
            verify(storageService).uploadImage(image, "recipes");
            verify(recipePersistence).save(any(Recipe.class));
        }

        @Test
        void shouldSaveRecipeWithEmptyImage() {
            var recipeId = UUID.randomUUID();
            var recipe = createRecipe(recipeId, "Salade", RecipeStatus.DRAFT);
            var image = mock(MultipartFile.class);
            when(image.isEmpty()).thenReturn(true);
            when(recipePersistence.save(any(Recipe.class))).thenReturn(recipe);

            var result = recipeService.createRecipe(recipe, image);

            assertNotNull(result);
            assertNull(result.imageUrl());
            verify(storageService, never()).uploadImage(any(), any());
            verify(recipePersistence).save(any(Recipe.class));
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