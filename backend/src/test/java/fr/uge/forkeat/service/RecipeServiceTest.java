package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipePersistence recipePersistence;

    private RecipeService recipeService;
    private Instant now;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(recipePersistence);
        now = Instant.now();
    }

    @Test
    void constructor_shouldThrowWhenPersistenceIsNull() {
        assertThrows(NullPointerException.class, () -> new RecipeService(null));
    }

    @Test
    void findById_shouldReturnRecipeWhenFound() {
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
    void findById_shouldThrowRecipeNotFoundExceptionWhenNotFound() {
        var recipeId = UUID.randomUUID();
        when(recipePersistence.findById(recipeId)).thenReturn(Optional.empty());

        var exception = assertThrows(RecipeNotFoundException.class,
                () -> recipeService.findById(recipeId));

        assertEquals(recipeId, exception.getRecipeId());
        verify(recipePersistence).findById(recipeId);
    }

    @Test
    void findByStatus_shouldReturnRecipesWithMatchingStatus() {
        var recipe1 = createRecipe(UUID.randomUUID(), "Recette 1", RecipeStatus.PUBLISHED);
        var recipe2 = createRecipe(UUID.randomUUID(), "Recette 2", RecipeStatus.PUBLISHED);
        when(recipePersistence.findByStatus("PUBLISHED")).thenReturn(List.of(recipe1, recipe2));

        var result = recipeService.findByStatus("PUBLISHED");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> r.status() == RecipeStatus.PUBLISHED));
        verify(recipePersistence).findByStatus("PUBLISHED");
    }

    @Test
    void findByStatus_shouldReturnEmptyListWhenNoRecipesMatch() {
        when(recipePersistence.findByStatus("DRAFT")).thenReturn(List.of());

        var result = recipeService.findByStatus("DRAFT");

        assertTrue(result.isEmpty());
        verify(recipePersistence).findByStatus("DRAFT");
    }

    @Test
    void findByStatus_shouldReturnDraftRecipes() {
        var draftRecipe = createRecipe(UUID.randomUUID(), "Brouillon", RecipeStatus.DRAFT);
        when(recipePersistence.findByStatus("DRAFT")).thenReturn(List.of(draftRecipe));

        var result = recipeService.findByStatus("DRAFT");

        assertEquals(1, result.size());
        assertEquals(RecipeStatus.DRAFT, result.getFirst().status());
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
