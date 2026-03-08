package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.UserAlreadyReportedException;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.user.CreateUserReport;
import fr.uge.forkeat.service.model.user.UserReport;
import fr.uge.forkeat.service.model.user.UserReportType;
import fr.uge.forkeat.service.persistence.UserReportPersistence;
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
class UserReportServiceTest {

    @Mock
    private UserReportPersistence userReportPersistence;
    @Mock
    private UserIdentityPort userIdentityPort;

    private UserReportService userReportService;

    @BeforeEach
    void setUp() {
        userReportService = new UserReportService(userReportPersistence, userIdentityPort);
    }

    private UserReport createReport(UUID reportedUserId, UUID reporterId) {
        return new UserReport(
                UUID.randomUUID(), reportedUserId, reporterId,
                UserReportType.SPAM, ReportStatus.PENDING,
                "Ceci est un spam", Instant.now(), Instant.now(), null, null
        );
    }

    @Nested
    class ReportUser {

        @Test
        void shouldCreateReportSuccessfully() {
            var reportedUserId = UUID.randomUUID();
            var reporterId = UUID.randomUUID();
            var command = new CreateUserReport("target", "reporter", UserReportType.SPAM, "Ceci est un spam");
            var expected = createReport(reportedUserId, reporterId);

            when(userIdentityPort.findIdByUsernameOrThrow("target")).thenReturn(reportedUserId);
            when(userIdentityPort.findIdByUsernameOrThrow("reporter")).thenReturn(reporterId);
            when(userReportPersistence.existsByReportedUserIdAndReporterId(reportedUserId, reporterId)).thenReturn(false);
            when(userReportPersistence.save(any())).thenReturn(expected);

            var result = userReportService.reportUser(command);

            assertNotNull(result);
            assertEquals(reportedUserId, result.reportedUserId());
            assertEquals(ReportStatus.PENDING, result.status());
            verify(userReportPersistence).save(any());
        }

        @Test
        void shouldThrowUserAlreadyReportedException_WhenAlreadyReported() {
            var reportedUserId = UUID.randomUUID();
            var reporterId = UUID.randomUUID();
            var command = new CreateUserReport("target", "reporter", UserReportType.HARASSMENT, "Justification");

            when(userIdentityPort.findIdByUsernameOrThrow("target")).thenReturn(reportedUserId);
            when(userIdentityPort.findIdByUsernameOrThrow("reporter")).thenReturn(reporterId);
            when(userReportPersistence.existsByReportedUserIdAndReporterId(reportedUserId, reporterId)).thenReturn(true);

            assertThrows(UserAlreadyReportedException.class, () -> userReportService.reportUser(command));
            verify(userReportPersistence, never()).save(any());
        }

        @Test
        void shouldThrowNullPointerException_WhenCommandIsNull() {
            assertThrows(NullPointerException.class, () -> userReportService.reportUser(null));
            verifyNoInteractions(userReportPersistence, userIdentityPort);
        }
    }

    @Nested
    class FindByStatus {

        @Test
        void shouldReturnReportsByStatus() {
            var reportedUserId = UUID.randomUUID();
            var reporterId = UUID.randomUUID();
            var reports = List.of(createReport(reportedUserId, reporterId), createReport(reportedUserId, reporterId));

            when(userReportPersistence.findByStatus(ReportStatus.PENDING)).thenReturn(reports);

            var result = userReportService.findByStatus(ReportStatus.PENDING);

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(r -> r.status() == ReportStatus.PENDING));
            verify(userReportPersistence).findByStatus(ReportStatus.PENDING);
        }

        @Test
        void shouldReturnEmptyList_WhenNoReports() {
            when(userReportPersistence.findByStatus(ReportStatus.VALIDATED)).thenReturn(List.of());

            var result = userReportService.findByStatus(ReportStatus.VALIDATED);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldThrowNullPointerException_WhenStatusIsNull() {
            assertThrows(NullPointerException.class, () -> userReportService.findByStatus(null));
            verifyNoInteractions(userReportPersistence);
        }
    }

    @Nested
    class FindByReportedUserId {

        @Test
        void shouldReturnReports_WhenUserExists() {
            var reportedUserId = UUID.randomUUID();
            var reporterId = UUID.randomUUID();
            var reports = List.of(createReport(reportedUserId, reporterId));

            when(userReportPersistence.findByReportedUserId(reportedUserId)).thenReturn(reports);

            var result = userReportService.findByReportedUserId(reportedUserId);

            assertEquals(1, result.size());
            assertEquals(reportedUserId, result.getFirst().reportedUserId());
        }

        @Test
        void shouldReturnEmptyList_WhenNoReports() {
            var reportedUserId = UUID.randomUUID();

            when(userReportPersistence.findByReportedUserId(reportedUserId)).thenReturn(List.of());

            var result = userReportService.findByReportedUserId(reportedUserId);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldThrowNullPointerException_WhenReportedUserIdIsNull() {
            assertThrows(NullPointerException.class, () -> userReportService.findByReportedUserId(null));
            verifyNoInteractions(userReportPersistence);
        }
    }
}