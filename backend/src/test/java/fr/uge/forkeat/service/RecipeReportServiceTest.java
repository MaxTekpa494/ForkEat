package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.RecipeAlreadyReportedException;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.recipe.CreateRecipeReport;
import fr.uge.forkeat.service.model.recipe.RecipeReport;
import fr.uge.forkeat.service.model.recipe.RecipeReportType;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.projection.RecipeReportDetails;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import fr.uge.forkeat.service.persistence.RecipeReportPersistence;
import fr.uge.forkeat.service.port.UserIdentityPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeReportServiceTest {

    @Mock
    private RecipeReportPersistence recipeReportPersistence;
    @Mock
    private RecipePersistence recipePersistence;
    @Mock
    private UserIdentityPort userIdentityPort;

    private RecipeReportService recipeReportService;

    @BeforeEach
    void setUp() {
        recipeReportService = new RecipeReportService(recipeReportPersistence, recipePersistence, userIdentityPort);
    }

    private RecipeReport createReport(UUID recipeId, UUID reporterId) {
        return new RecipeReport(
                UUID.randomUUID(), recipeId, reporterId,
                RecipeReportType.SPAM, ReportStatus.PENDING,
                "Ceci est un spam", Instant.now(), null, null
        );
    }

    @Nested
    class ReportRecipe {

        @Test
        void shouldCreateReportSuccessfully() {
            var recipeId = UUID.randomUUID();
            var reporterId = UUID.randomUUID();
            var command = new CreateRecipeReport(recipeId, "reporter", RecipeReportType.SPAM, "Ceci est un spam");
            var expected = createReport(recipeId, reporterId);

            when(recipePersistence.existRecipe(recipeId)).thenReturn(true);
            when(userIdentityPort.findIdByUsernameOrThrow("reporter")).thenReturn(reporterId);
            when(recipeReportPersistence.existsByRecipeIdAndReporterId(recipeId, reporterId)).thenReturn(false);
            when(recipeReportPersistence.save(any())).thenReturn(expected);

            var result = recipeReportService.reportRecipe(command);

            assertNotNull(result);
            assertEquals(recipeId, result.recipeId());
            assertEquals(ReportStatus.PENDING, result.status());
            verify(recipeReportPersistence).save(any());
        }

        @Test
        void shouldThrowRecipeNotFoundException_WhenRecipeDoesNotExist() {
            var recipeId = UUID.randomUUID();
            var command = new CreateRecipeReport(recipeId, "reporter", RecipeReportType.SPAM, "Justification");

            when(recipePersistence.existRecipe(recipeId)).thenReturn(false);

            assertThrows(RecipeNotFoundException.class, () -> recipeReportService.reportRecipe(command));
            verify(recipeReportPersistence, never()).save(any());
        }

        @Test
        void shouldThrowRecipeAlreadyReportedException_WhenAlreadyReported() {
            var recipeId = UUID.randomUUID();
            var reporterId = UUID.randomUUID();
            var command = new CreateRecipeReport(recipeId, "reporter", RecipeReportType.SPAM, "Justification");

            when(recipePersistence.existRecipe(recipeId)).thenReturn(true);
            when(userIdentityPort.findIdByUsernameOrThrow("reporter")).thenReturn(reporterId);
            when(recipeReportPersistence.existsByRecipeIdAndReporterId(recipeId, reporterId)).thenReturn(true);

            assertThrows(RecipeAlreadyReportedException.class, () -> recipeReportService.reportRecipe(command));
            verify(recipeReportPersistence, never()).save(any());
        }

        @Test
        void shouldThrowNullPointerException_WhenCommandIsNull() {
            assertThrows(NullPointerException.class, () -> recipeReportService.reportRecipe(null));
            verifyNoInteractions(recipeReportPersistence, recipePersistence, userIdentityPort);
        }
    }

    @Nested
    class FindByStatus {

        @Test
        void shouldReturnReportsByStatus() {
            var recipeId = UUID.randomUUID();
            var reporterId = UUID.randomUUID();
            var reports = List.of(createReport(recipeId, reporterId), createReport(recipeId, reporterId));

            when(recipeReportPersistence.findByStatus(ReportStatus.PENDING)).thenReturn(reports);

            var result = recipeReportService.findByStatus(ReportStatus.PENDING);

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(r -> r.status() == ReportStatus.PENDING));
            verify(recipeReportPersistence).findByStatus(ReportStatus.PENDING);
        }

        @Test
        void shouldReturnEmptyList_WhenNoReports() {
            when(recipeReportPersistence.findByStatus(ReportStatus.VALIDATED)).thenReturn(List.of());

            var result = recipeReportService.findByStatus(ReportStatus.VALIDATED);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldThrowNullPointerException_WhenStatusIsNull() {
            assertThrows(NullPointerException.class, () -> recipeReportService.findByStatus(null));
            verifyNoInteractions(recipeReportPersistence);
        }
    }

    @Nested
    class FindByRecipeId {

        @Test
        void shouldReturnReports_WhenRecipeExists() {
            var recipeId = UUID.randomUUID();
            var reporterId = UUID.randomUUID();
            var reports = List.of(createReport(recipeId, reporterId));

            when(recipePersistence.existRecipe(recipeId)).thenReturn(true);
            when(recipeReportPersistence.findByRecipeId(recipeId)).thenReturn(reports);

            var result = recipeReportService.findByRecipeId(recipeId);

            assertEquals(1, result.size());
            assertEquals(recipeId, result.getFirst().recipeId());
        }

        @Test
        void shouldThrowRecipeNotFoundException_WhenRecipeDoesNotExist() {
            var recipeId = UUID.randomUUID();

            when(recipePersistence.existRecipe(recipeId)).thenReturn(false);

            assertThrows(RecipeNotFoundException.class, () -> recipeReportService.findByRecipeId(recipeId));
            verify(recipeReportPersistence, never()).findByRecipeId(any());
        }

        @Test
        void shouldThrowNullPointerException_WhenRecipeIdIsNull() {
            assertThrows(NullPointerException.class, () -> recipeReportService.findByRecipeId(null));
            verifyNoInteractions(recipeReportPersistence, recipePersistence);
        }
    }

    @Nested
    class GetReportsToModerate {
        @Test
        void shouldReturnReportsToModerate() {
            var reporterUsername = "moderator";
            var reporterId = UUID.randomUUID();
            int size = 5;
            int page = 0;
            var details = new RecipeReportDetails(
                    UUID.randomUUID(), UUID.randomUUID(), "Tarte aux pommes", "img.jpg",
                    "user1", "SPAM", "Justification", Instant.now()
            );
            var pageResult = new PageResult<>(List.of(details), 1);

            when(userIdentityPort.findIdByUsernameOrThrow(reporterUsername)).thenReturn(reporterId);
            when(recipeReportPersistence.getReportsToModerateWithRecipeAndReporter(reporterId, size, page)).thenReturn(pageResult);

            var result = recipeReportService.getReportsToModerate(reporterUsername, size, page);

            assertNotNull(result);
            assertEquals(1, result.items().size());
            assertEquals(1, result.total());
            assertEquals("Tarte aux pommes", result.items().getFirst().recipeTitle());
            verify(userIdentityPort).findIdByUsernameOrThrow(reporterUsername);
            verify(recipeReportPersistence).getReportsToModerateWithRecipeAndReporter(reporterId, size, page);
        }

        @Test
        void shouldReturnEmptyPage_WhenNoReports() {
            var reporterUsername = "moderator";
            var reporterId = UUID.randomUUID();
            int size = 5;
            int page = 0;
            var pageResult = new PageResult<RecipeReportDetails>(List.of(), 0);

            when(userIdentityPort.findIdByUsernameOrThrow(reporterUsername)).thenReturn(reporterId);
            when(recipeReportPersistence.getReportsToModerateWithRecipeAndReporter(reporterId, size, page)).thenReturn(pageResult);

            var result = recipeReportService.getReportsToModerate(reporterUsername, size, page);

            assertNotNull(result);
            assertTrue(result.items().isEmpty());
            assertEquals(0, result.total());
        }

        @Test
        void shouldThrowNullPointerException_WhenUsernameIsNull() {
            assertThrows(NullPointerException.class, () -> recipeReportService.getReportsToModerate(null, 5, 0));
            verifyNoInteractions(recipeReportPersistence);
        }

        @Test
        void shouldThrowIllegalArgumentException_WhenSizeOrPageInvalid() {
            var reporterUsername = "moderator";
            when(userIdentityPort.findIdByUsernameOrThrow(reporterUsername)).thenReturn(UUID.randomUUID());
            assertThrows(IllegalArgumentException.class, () -> recipeReportService.getReportsToModerate(reporterUsername, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> recipeReportService.getReportsToModerate(reporterUsername, 5, -1));
        }
    }
}