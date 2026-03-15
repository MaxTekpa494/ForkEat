package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeReportEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeReportRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.recipe.RecipeReport;
import fr.uge.forkeat.service.model.recipe.RecipeReportType;
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
class RecipeReportPersistenceAdapterTest {

    @Mock private RecipeReportRepository recipeReportRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private UserRepository userRepository;

    private RecipeReportPersistenceAdapter adapter;
    private UserEntity reporter;
    private RecipeEntity recipe;
    private Instant now;

    @BeforeEach
    void setUp() {
        adapter = new RecipeReportPersistenceAdapter(recipeReportRepository, recipeRepository, userRepository);
        now = Instant.now();

        reporter = new UserEntity();
        reporter.setId(UUID.randomUUID());
        reporter.setUsername("reporter");
        reporter.setFirstName("Jean");
        reporter.setLastName("Dupont");
        reporter.setEmail("reporter@test.com");
        reporter.setPassword("hashed");
        reporter.setRole(UserRole.MEMBER);
        reporter.setStatus(UserStatus.ACTIVE);
        reporter.setAuthMode(AuthMode.LOCAL);

        recipe = new RecipeEntity();
        recipe.setId(UUID.randomUUID());
        recipe.setTitle("Tarte aux pommes");
        recipe.setSummary("Une délicieuse tarte");
        recipe.setAuthor(reporter);
        recipe.setStatus(RecipeStatus.PUBLISHED);
        recipe.setStepByStepInstructions(List.of());
        recipe.setCreatedAt(now);
        recipe.setUpdatedAt(now);
    }

    private RecipeReportEntity createReportEntity(UUID recipeId, UUID reporterId) {
        var entity = new RecipeReportEntity();
        entity.setId(UUID.randomUUID());
        entity.setRecipe(recipe);
        entity.setReporter(reporter);
        entity.setReportType(RecipeReportType.SPAM);
        entity.setStatus(ReportStatus.PENDING);
        entity.setJustification("Justification");
        return entity;
    }

    @Nested
    class Save {

        @Test
        void shouldPersistReport() {
            var report = new RecipeReport(
                    UUID.randomUUID(), recipe.getId(), reporter.getId(),
                    RecipeReportType.SPAM, ReportStatus.PENDING, "Justification", now, null, null
            );
            var savedEntity = createReportEntity(recipe.getId(), reporter.getId());

            when(recipeRepository.getReferenceById(recipe.getId())).thenReturn(recipe);
            when(userRepository.getReferenceById(reporter.getId())).thenReturn(reporter);
            when(recipeReportRepository.save(any())).thenReturn(savedEntity);

            var result = adapter.save(report);

            assertNotNull(result);
            verify(recipeReportRepository).save(any());
        }

        @Test
        void shouldNotFetchReporter_WhenReporterIdIsNull() {
            var report = new RecipeReport(
                    UUID.randomUUID(), recipe.getId(), null,
                    RecipeReportType.SPAM, ReportStatus.PENDING, "Justification", now, null, null
            );
            var savedEntity = createReportEntity(recipe.getId(), null);

            when(recipeRepository.getReferenceById(recipe.getId())).thenReturn(recipe);
            when(recipeReportRepository.save(any())).thenReturn(savedEntity);

            adapter.save(report);

            verify(userRepository, never()).getReferenceById(any());
        }
    }

    @Nested
    class FindByRecipeId {

        @Test
        void shouldReturnReports() {
            var entity = createReportEntity(recipe.getId(), reporter.getId());
            when(recipeReportRepository.findByRecipeId(recipe.getId())).thenReturn(List.of(entity));

            var result = adapter.findByRecipeId(recipe.getId());

            assertEquals(1, result.size());
            verify(recipeReportRepository).findByRecipeId(recipe.getId());
        }

        @Test
        void shouldReturnEmptyList_WhenNoReports() {
            when(recipeReportRepository.findByRecipeId(recipe.getId())).thenReturn(List.of());

            var result = adapter.findByRecipeId(recipe.getId());

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class FindByStatus {

        @Test
        void shouldReturnPendingReports() {
            var entity = createReportEntity(recipe.getId(), reporter.getId());
            when(recipeReportRepository.findByStatus(ReportStatus.PENDING)).thenReturn(List.of(entity));

            var result = adapter.findByStatus(ReportStatus.PENDING);

            assertEquals(1, result.size());
            verify(recipeReportRepository).findByStatus(ReportStatus.PENDING);
        }
    }

    @Nested
    class ExistsById{

        @Test
        void shouldReturnTrue_WhenReportExists() {
            when(recipeReportRepository.existsById(recipe.getId()))
                    .thenReturn(true);

            assertTrue(adapter.existsById(recipe.getId()));
        }

        @Test
        void shouldReturnFalse_WhenReportDoesNotExist() {
            when(recipeReportRepository.existsById(recipe.getId()))
                    .thenReturn(false);

            assertFalse(adapter.existsById(recipe.getId()));
        }
    }

    @Nested
    class ExistsByRecipeIdAndReporterId {

        @Test
        void shouldReturnTrue_WhenReportExists() {
            when(recipeReportRepository.existsByRecipeIdAndReporterId(recipe.getId(), reporter.getId()))
                    .thenReturn(true);

            assertTrue(adapter.existsByRecipeIdAndReporterId(recipe.getId(), reporter.getId()));
        }

        @Test
        void shouldReturnFalse_WhenReportDoesNotExist() {
            when(recipeReportRepository.existsByRecipeIdAndReporterId(recipe.getId(), reporter.getId()))
                    .thenReturn(false);

            assertFalse(adapter.existsByRecipeIdAndReporterId(recipe.getId(), reporter.getId()));
        }
    }
}
