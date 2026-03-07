package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserReportEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserReportRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.user.UserReport;
import fr.uge.forkeat.service.model.user.UserReportType;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserReportPersistenceAdapterTest {

    @Mock private UserReportRepository userReportRepository;
    @Mock private UserRepository userRepository;

    private UserReportPersistenceAdapter adapter;
    private UserEntity reportedUser;
    private UserEntity reporter;
    private Instant now;

    @BeforeEach
    void setUp() {
        adapter = new UserReportPersistenceAdapter(userReportRepository, userRepository);
        now = Instant.now();

        reportedUser = new UserEntity();
        reportedUser.setId(UUID.randomUUID());
        reportedUser.setUsername("target");
        reportedUser.setFirstName("Alice");
        reportedUser.setLastName("Martin");
        reportedUser.setEmail("target@test.com");
        reportedUser.setPassword("hashed");
        reportedUser.setRole(UserRole.MEMBER);
        reportedUser.setStatus(UserStatus.ACTIVE);
        reportedUser.setAuthMode(AuthMode.LOCAL);

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
    }

    private UserReportEntity createReportEntity() {
        var entity = new UserReportEntity();
        entity.setId(UUID.randomUUID());
        entity.setReportedUser(reportedUser);
        entity.setReporter(reporter);
        entity.setReportType(UserReportType.SPAM);
        entity.setStatus(ReportStatus.PENDING);
        entity.setJustification("Justification");
        return entity;
    }

    private UserReport createReport(UUID reportedUserId, UUID reporterId) {
        return new UserReport(
                UUID.randomUUID(), reportedUserId, reporterId,
                UserReportType.SPAM, ReportStatus.PENDING,
                "Justification", now, now, null, null
        );
    }

    @Nested
    class Save {

        @Test
        void shouldPersistReport() {
            var report = createReport(reportedUser.getId(), reporter.getId());
            var savedEntity = createReportEntity();

            when(userRepository.findById(reportedUser.getId())).thenReturn(Optional.of(reportedUser));
            when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
            when(userReportRepository.save(any())).thenReturn(savedEntity);

            var result = adapter.save(report);

            assertNotNull(result);
            verify(userReportRepository).save(any());
        }

        @Test
        void shouldThrowIllegalState_WhenReportedUserNotFound() {
            var report = createReport(reportedUser.getId(), reporter.getId());

            when(userRepository.findById(reportedUser.getId())).thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class, () -> adapter.save(report));
            verify(userReportRepository, never()).save(any());
        }

        @Test
        void shouldThrowIllegalState_WhenReporterNotFound() {
            var report = createReport(reportedUser.getId(), reporter.getId());

            when(userRepository.findById(reportedUser.getId())).thenReturn(Optional.of(reportedUser));
            when(userRepository.findById(reporter.getId())).thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class, () -> adapter.save(report));
            verify(userReportRepository, never()).save(any());
        }

        @Test
        void shouldNotFetchReporter_WhenReporterIdIsNull() {
            var report = createReport(reportedUser.getId(), null);
            var savedEntity = createReportEntity();

            when(userRepository.findById(reportedUser.getId())).thenReturn(Optional.of(reportedUser));
            when(userReportRepository.save(any())).thenReturn(savedEntity);

            adapter.save(report);

            verify(userRepository, times(1)).findById(any());
        }
    }

    @Nested
    class FindByReportedUserId {

        @Test
        void shouldReturnReports() {
            var entity = createReportEntity();
            when(userReportRepository.findByReportedUserId(reportedUser.getId())).thenReturn(List.of(entity));

            var result = adapter.findByReportedUserId(reportedUser.getId());

            assertEquals(1, result.size());
            verify(userReportRepository).findByReportedUserId(reportedUser.getId());
        }

        @Test
        void shouldReturnEmptyList_WhenNoReports() {
            when(userReportRepository.findByReportedUserId(reportedUser.getId())).thenReturn(List.of());

            var result = adapter.findByReportedUserId(reportedUser.getId());

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class FindByStatus {

        @Test
        void shouldReturnPendingReports() {
            var entity = createReportEntity();
            when(userReportRepository.findByStatus(ReportStatus.PENDING)).thenReturn(List.of(entity));

            var result = adapter.findByStatus(ReportStatus.PENDING);

            assertEquals(1, result.size());
            verify(userReportRepository).findByStatus(ReportStatus.PENDING);
        }
    }

    @Nested
    class ExistsByReportedUserIdAndReporterId {

        @Test
        void shouldReturnTrue_WhenReportExists() {
            when(userReportRepository.existsByReportedUserIdAndReporterId(reportedUser.getId(), reporter.getId()))
                    .thenReturn(true);

            assertTrue(adapter.existsByReportedUserIdAndReporterId(reportedUser.getId(), reporter.getId()));
        }

        @Test
        void shouldReturnFalse_WhenReportDoesNotExist() {
            when(userReportRepository.existsByReportedUserIdAndReporterId(reportedUser.getId(), reporter.getId()))
                    .thenReturn(false);

            assertFalse(adapter.existsByReportedUserIdAndReporterId(reportedUser.getId(), reporter.getId()));
        }
    }
}