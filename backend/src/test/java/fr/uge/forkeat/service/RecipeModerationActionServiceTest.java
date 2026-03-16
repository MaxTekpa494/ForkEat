package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.ModeratorIsAuthorException;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.persistence.RecipeModerationActionPersistence;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import fr.uge.forkeat.service.persistence.RecipeReportPersistence;
import fr.uge.forkeat.service.port.UserIdentityPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeModerationActionServiceTest {

    @Mock
    private RecipeModerationActionPersistence moderationActionPersistence;
    @Mock
    private RecipePersistence recipePersistence;
    @Mock
    private RecipeReportPersistence recipeReportPersistence;
    @Mock
    private UserIdentityPort userIdentityPort;
    @Mock
    private RecipeService recipeService;

    private RecipeModerationActionService recipeModerationActionService;

    @BeforeEach
    void setUp() {
        recipeModerationActionService = new RecipeModerationActionService(moderationActionPersistence, recipePersistence, recipeReportPersistence, userIdentityPort, recipeService);
    }

    // TODO : à changer quand on fera signalement avec relatedreport != null
    private RecipeModerationAction createModerationAction(UUID recipeId, UUID moderatorId, RecipeModerationActionType moderationActionType, String justification) {
        return new RecipeModerationAction(
                UUID.randomUUID(), recipeId, moderatorId, moderationActionType, justification, Instant.now(), null, null
        );
    }


    @Nested
    class ModerateRecipe {
        @Test
        void shouldCreateModerationActionSuccessfully() {
            var recipeId = UUID.randomUUID();
            var moderatorId = UUID.randomUUID();
            var command = new CreateRecipeModerationAction(recipeId, "mod", RecipeModerationActionType.REJECTED, "Justification", null);
            var expected = createModerationAction(recipeId, moderatorId, RecipeModerationActionType.REJECTED, "Justification");

            when(recipePersistence.existRecipe(recipeId)).thenReturn(true);

            when(userIdentityPort.findIdByUsernameOrThrow("mod")).thenReturn(moderatorId);
            when(moderationActionPersistence.save(any())).thenReturn(expected);
            var recipe = createRecipe(recipeId, "joe");
            when(recipeService.findById(recipeId)).thenReturn(recipe);

            var result = recipeModerationActionService.moderateRecipe(command);

            assertNotNull(result);
            assertEquals(recipeId, result.recipeId());
            assertEquals(RecipeModerationActionType.REJECTED, result.moderationActionType());
            assertEquals("Justification", result.justification());
            verify(moderationActionPersistence).save(any());
        }

        @Test
        void shouldThrowRecipeNotFoundException_WhenRecipeDoesNotExist() {
            var recipeId = UUID.randomUUID();
            var command = new CreateRecipeModerationAction(recipeId, "mod", RecipeModerationActionType.APPROVED, "Justification", null);

            when(recipePersistence.existRecipe(recipeId)).thenReturn(false);

            assertThrows(RecipeNotFoundException.class, () -> recipeModerationActionService.moderateRecipe(command));
            verify(moderationActionPersistence, never()).save(any());
        }

        @Test
        void shouldThrowNullPointerException_WhenCommandIsNull() {
            assertThrows(NullPointerException.class, () -> recipeModerationActionService.moderateRecipe(null));
            verifyNoInteractions(moderationActionPersistence, recipePersistence, userIdentityPort);
        }

        @Test
        void shouldThrowIllegalStateException_WhenModeratorIsAuthor() {
            var recipeId = UUID.randomUUID();
            var moderatorId = UUID.randomUUID();
            var command = new CreateRecipeModerationAction(recipeId, "mod", RecipeModerationActionType.REJECTED, "Justification", null);

            when(recipePersistence.existRecipe(recipeId)).thenReturn(true);
            when(userIdentityPort.findIdByUsernameOrThrow("mod")).thenReturn(moderatorId);
            var recipe = createRecipe(recipeId, "mod");
            when(recipeService.findById(recipeId)).thenReturn(recipe);

            assertThrows(ModeratorIsAuthorException.class, () -> recipeModerationActionService.moderateRecipe(command));
            verify(moderationActionPersistence, never()).save(any());
        }
    }

    @Nested
    class FindByType {
        @Test
        void shouldReturnModerationActionsByType() {
            var recipeId = UUID.randomUUID();
            var moderatorId = UUID.randomUUID();
            var actions = List.of(createModerationAction(recipeId, moderatorId, RecipeModerationActionType.REJECTED, "Justification"));

            when(moderationActionPersistence.findByActionType(RecipeModerationActionType.REJECTED)).thenReturn(actions);

            var result = recipeModerationActionService.findByType(RecipeModerationActionType.REJECTED);

            assertEquals(1, result.size());
            assertTrue(result.stream().allMatch(a -> a.moderationActionType() == RecipeModerationActionType.REJECTED));
            verify(moderationActionPersistence).findByActionType(RecipeModerationActionType.REJECTED);
        }

        @Test
        void shouldReturnEmptyList_WhenNoActions() {
            when(moderationActionPersistence.findByActionType(RecipeModerationActionType.APPROVED)).thenReturn(List.of());

            var result = recipeModerationActionService.findByType(RecipeModerationActionType.APPROVED);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldThrowNullPointerException_WhenTypeIsNull() {
            assertThrows(NullPointerException.class, () -> recipeModerationActionService.findByType(null));
            verifyNoInteractions(moderationActionPersistence);
        }
    }

    @Nested
    class FindByRecipeId {
        @Test
        void shouldReturnModerationActions_WhenRecipeExists() {
            var recipeId = UUID.randomUUID();
            var moderatorId = UUID.randomUUID();
            var actions = List.of(createModerationAction(recipeId, moderatorId, RecipeModerationActionType.REJECTED, "Justification"));

            when(recipePersistence.existRecipe(recipeId)).thenReturn(true);
            when(moderationActionPersistence.findByRecipeId(recipeId)).thenReturn(actions);

            var result = recipeModerationActionService.findByRecipeId(recipeId);

            assertEquals(1, result.size());
            assertEquals(recipeId, result.getFirst().recipeId());
        }

        @Test
        void shouldThrowRecipeNotFoundException_WhenRecipeDoesNotExist() {
            var recipeId = UUID.randomUUID();

            when(recipePersistence.existRecipe(recipeId)).thenReturn(false);

            assertThrows(RecipeNotFoundException.class, () -> recipeModerationActionService.findByRecipeId(recipeId));
            verify(moderationActionPersistence, never()).findByRecipeId(any());
        }

        @Test
        void shouldThrowNullPointerException_WhenRecipeIdIsNull() {
            assertThrows(NullPointerException.class, () -> recipeModerationActionService.findByRecipeId(null));
            verifyNoInteractions(moderationActionPersistence, recipePersistence);
        }
    }

    private Recipe createRecipe(UUID id, String usernameAuthor) {
        return new Recipe(
                id,
                "Titre test",
                "Résumé test",
                null,
                usernameAuthor,
                30,
                "",
                RecipeStatus.PENDING_REVIEW,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
