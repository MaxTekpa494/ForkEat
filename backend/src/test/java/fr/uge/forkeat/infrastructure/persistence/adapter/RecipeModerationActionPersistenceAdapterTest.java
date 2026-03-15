package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeModerationActionEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeReportEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeModerationActionRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeReportRepository;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.recipe.RecipeModerationAction;
import fr.uge.forkeat.service.model.recipe.RecipeModerationActionType;
import fr.uge.forkeat.service.model.recipe.RecipeReportType;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
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
class RecipeModerationActionPersistenceAdapterTest {

    @Mock private RecipeModerationActionRepository moderationActionRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private UserRepository userRepository;
    @Mock private RecipeReportRepository recipeReportRepository;

    private RecipeModerationActionPersistenceAdapter adapter;
    private UserEntity moderator;
    private RecipeEntity recipe;
    private RecipeReportEntity relatedReport;
    private Instant now;

    @BeforeEach
    void setUp() {
        adapter = new RecipeModerationActionPersistenceAdapter(moderationActionRepository, recipeRepository, userRepository, recipeReportRepository);
        now = Instant.now();

        moderator = new UserEntity();
        moderator.setId(UUID.randomUUID());
        moderator.setUsername("mod");
        moderator.setFirstName("Jean");
        moderator.setLastName("Modérateur");
        moderator.setEmail("mod@test.com");
        moderator.setPassword("hashed");
        moderator.setRole(UserRole.MODERATOR);
        moderator.setStatus(UserStatus.ACTIVE);
        moderator.setAuthMode(AuthMode.LOCAL);

        recipe = new RecipeEntity();
        recipe.setId(UUID.randomUUID());
        recipe.setTitle("Tarte tatin");
        recipe.setSummary("Délicieuse tarte tatin");
        recipe.setAuthor(moderator);
        recipe.setStatus(RecipeStatus.PUBLISHED);
        recipe.setStepByStepInstructions(List.of());
        recipe.setCreatedAt(now);
        recipe.setUpdatedAt(now);

        relatedReport = new RecipeReportEntity();
        relatedReport.setId(UUID.randomUUID());
        relatedReport.setRecipe(recipe);
        relatedReport.setReporter(moderator);
        relatedReport.setReportType(RecipeReportType.SPAM);
        relatedReport.setStatus(ReportStatus.PENDING);
        relatedReport.setJustification("Spam");
    }

    private RecipeModerationActionEntity createModerationActionEntity() {
        var entity = new RecipeModerationActionEntity();
        entity.setId(UUID.randomUUID());
        entity.setRecipe(recipe);
        entity.setModerator(moderator);
        entity.setModerationActionType(RecipeModerationActionType.REJECTED);
        entity.setJustification("Justification");
        entity.setUpdatedAt(null);
        entity.setRelatedReport(relatedReport);
        return entity;
    }

    @Nested
    class Save {
        @Test
        void shouldPersistModerationAction() {
            var moderationAction = new RecipeModerationAction(
                    UUID.randomUUID(), recipe.getId(), moderator.getId(),
                    RecipeModerationActionType.REJECTED, "Justification", now, null, relatedReport.getId()
            );
            var savedEntity = createModerationActionEntity();

            when(recipeRepository.getReferenceById(recipe.getId())).thenReturn(recipe);
            when(userRepository.getReferenceById(moderator.getId())).thenReturn(moderator);
            when(recipeReportRepository.getReferenceById(relatedReport.getId())).thenReturn(relatedReport);
            when(moderationActionRepository.save(any())).thenReturn(savedEntity);

            var result = adapter.save(moderationAction);

            assertNotNull(result);
            verify(moderationActionRepository).save(any());
        }

        @Test
        void shouldNotFetchRelatedReport_WhenRelatedReportIdIsNull() {
            var moderationAction = new RecipeModerationAction(
                    UUID.randomUUID(), recipe.getId(), moderator.getId(),
                    RecipeModerationActionType.REJECTED, "Justification", now, null, null
            );
            var savedEntity = createModerationActionEntity();

            when(recipeRepository.getReferenceById(recipe.getId())).thenReturn(recipe);
            when(userRepository.getReferenceById(moderator.getId())).thenReturn(moderator);
            when(moderationActionRepository.save(any())).thenReturn(savedEntity);

            adapter.save(moderationAction);

            verify(recipeReportRepository, never()).getReferenceById(any());
        }
    }

    @Nested
    class FindByRecipeId {
        @Test
        void shouldReturnModerationActions() {
            var entity = createModerationActionEntity();
            when(moderationActionRepository.findByRecipeId(recipe.getId())).thenReturn(List.of(entity));

            var result = adapter.findByRecipeId(recipe.getId());

            assertEquals(1, result.size());
            verify(moderationActionRepository).findByRecipeId(recipe.getId());
        }

        @Test
        void shouldReturnEmptyList_WhenNoActions() {
            when(moderationActionRepository.findByRecipeId(recipe.getId())).thenReturn(List.of());

            var result = adapter.findByRecipeId(recipe.getId());

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class FindByType {
        @Test
        void shouldReturnApprovedActions() {
          var entity = createModerationActionEntity();
          when(moderationActionRepository.findByModerationActionType(RecipeModerationActionType.APPROVED)).thenReturn(List.of(entity));

          var result = adapter.findByActionType(RecipeModerationActionType.APPROVED);

          assertEquals(1, result.size());
          verify(moderationActionRepository).findByModerationActionType(RecipeModerationActionType.APPROVED);
        }

        @Test
        void shouldReturnRejectedActions() {
            var entity = createModerationActionEntity();
            when(moderationActionRepository.findByModerationActionType(RecipeModerationActionType.REJECTED)).thenReturn(List.of(entity));

            var result = adapter.findByActionType(RecipeModerationActionType.REJECTED);

            assertEquals(1, result.size());
            verify(moderationActionRepository).findByModerationActionType(RecipeModerationActionType.REJECTED);
        }
    }
}

