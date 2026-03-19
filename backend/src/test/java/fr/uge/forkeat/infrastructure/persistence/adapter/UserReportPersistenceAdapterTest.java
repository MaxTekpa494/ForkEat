package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserReportEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.UserReportDetailsView;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserReportRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.user.UserReport;
import fr.uge.forkeat.service.model.user.UserReportType;
import fr.uge.forkeat.service.model.user.projection.UserReportDetails;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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

            when(userRepository.getReferenceById(reportedUser.getId())).thenReturn(reportedUser);
            when(userRepository.getReferenceById(reporter.getId())).thenReturn(reporter);
            when(userReportRepository.save(any())).thenReturn(savedEntity);

            var result = adapter.save(report);

            assertNotNull(result);
            verify(userReportRepository).save(any());
        }

        @Test
        void shouldNotFetchReporter_WhenReporterIdIsNull() {
            var report = createReport(reportedUser.getId(), null);
            var savedEntity = createReportEntity();

            when(userRepository.getReferenceById(reportedUser.getId())).thenReturn(reportedUser);
            when(userReportRepository.save(any())).thenReturn(savedEntity);

            adapter.save(report);

            verify(userRepository, times(1)).getReferenceById(any());
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

    @Nested
    class ExistsById {
        @Test
        void shouldReturnTrue_WhenReportExists() {
            UUID reportId = UUID.randomUUID();
            when(userReportRepository.existsById(reportId)).thenReturn(true);
            assertTrue(adapter.existsById(reportId));
        }

        @Test
        void shouldReturnFalse_WhenReportDoesNotExist() {
            UUID reportId = UUID.randomUUID();
            when(userReportRepository.existsById(reportId)).thenReturn(false);
            assertFalse(adapter.existsById(reportId));
        }

        @Test
        void shouldThrow_WhenReportIdIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.existsById(null));
        }
    }

    @Nested
    class IsAuthor {
        @Test
        void shouldReturnTrue_WhenUserIsAuthor() {
            UUID reportId = UUID.randomUUID();
            UUID reporterId = UUID.randomUUID();
            when(userReportRepository.existsByIdAndReporterId(reportId, reporterId)).thenReturn(true);
            assertTrue(adapter.isAuthor(reportId, reporterId));
        }

        @Test
        void shouldReturnFalse_WhenUserIsNotAuthor() {
            UUID reportId = UUID.randomUUID();
            UUID reporterId = UUID.randomUUID();
            when(userReportRepository.existsByIdAndReporterId(reportId, reporterId)).thenReturn(false);
            assertFalse(adapter.isAuthor(reportId, reporterId));
        }

        @Test
        void shouldThrow_WhenReportIdIsNull() {
            UUID reporterId = UUID.randomUUID();
            assertThrows(NullPointerException.class, () -> adapter.isAuthor(null, reporterId));
        }

        @Test
        void shouldThrow_WhenReporterIdIsNull() {
            UUID reportId = UUID.randomUUID();
            assertThrows(NullPointerException.class, () -> adapter.isAuthor(reportId, null));
        }
    }

    @Nested
    class UpdateStatus {
        @Test
        void shouldUpdateStatusSuccessfully() {
            UUID reportId = UUID.randomUUID();
            UUID reviewerId = UUID.randomUUID();
            var entity = createReportEntity();
            entity.setId(reportId);
            var reviewer = new UserEntity();
            reviewer.setId(reviewerId);
            when(userReportRepository.findById(reportId)).thenReturn(Optional.of(entity));
            when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
            when(userReportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var result = adapter.updateStatus(reportId, reviewerId, ReportStatus.VALIDATED);

            assertNotNull(result);
            assertEquals(ReportStatus.VALIDATED, result.status());
            assertEquals(reviewerId, entity.getReviewedBy().getId());
            assertNotNull(entity.getReviewedAt());
        }

        @Test
        void shouldThrow_WhenReportNotFound() {
            UUID reportId = UUID.randomUUID();
            UUID reviewerId = UUID.randomUUID();
            when(userReportRepository.findById(reportId)).thenReturn(Optional.empty());
            assertThrows(fr.uge.forkeat.service.exception.ResourceNotFoundException.class,
                () -> adapter.updateStatus(reportId, reviewerId, ReportStatus.DISMISSED));
        }

        @Test
        void shouldThrow_WhenReviewerNotFound() {
            UUID reportId = UUID.randomUUID();
            UUID reviewerId = UUID.randomUUID();
            var entity = createReportEntity();
            entity.setId(reportId);
            when(userReportRepository.findById(reportId)).thenReturn(Optional.of(entity));
            when(userRepository.findById(reviewerId)).thenReturn(Optional.empty());
            assertThrows(fr.uge.forkeat.service.exception.ResourceNotFoundException.class,
                () -> adapter.updateStatus(reportId, reviewerId, ReportStatus.DISMISSED));
        }

        @Test
        void shouldThrow_WhenReportIdIsNull() {
            UUID reviewerId = UUID.randomUUID();
            assertThrows(NullPointerException.class, () -> adapter.updateStatus(null, reviewerId, ReportStatus.PENDING));
        }

        @Test
        void shouldThrow_WhenReviewerIdIsNull() {
            UUID reportId = UUID.randomUUID();
            assertThrows(NullPointerException.class, () -> adapter.updateStatus(reportId, null, ReportStatus.PENDING));
        }

        @Test
        void shouldThrow_WhenStatusIsNull() {
            UUID reportId = UUID.randomUUID();
            UUID reviewerId = UUID.randomUUID();
            assertThrows(NullPointerException.class, () -> adapter.updateStatus(reportId, reviewerId, null));
        }
    }

    @Nested
    class GetReportsToModerateWithReportedUserAndReporter {
        @Test
        void shouldReturnReportsToModerate() {
            var reporterId = UUID.randomUUID();
            var reportedId = UUID.randomUUID();
            int page = 0;
            int size = 10;
            var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            var details = new UserReportDetails(
                    UUID.randomUUID(),
                    reportedId,
                    "reported",
                    "reporter",
                    "SPAM",
                    "Justification",
                    now
            );
            var view = createUserReportDetailsView(details);
            Page<UserReportDetailsView> pageResult = new PageImpl<>(List.of(view), pageable, 1);

            when(userReportRepository.findUserReportsByStatusAndNotReporterIdWithReportedUserAndReporter(
                    eq(ReportStatus.PENDING), eq(reporterId), eq(pageable)
            )).thenReturn(pageResult);

            PageResult<UserReportDetails> result = adapter.getReportsToModerateWithReportedUserAndReporter(reporterId, size, page);

            assertNotNull(result);
            assertEquals(1, result.items().size());
            assertEquals(details.id(), result.items().getFirst().id());
            assertEquals(reportedId, result.items().getFirst().reportedUserId());
            verify(userReportRepository).findUserReportsByStatusAndNotReporterIdWithReportedUserAndReporter(
                    eq(ReportStatus.PENDING), eq(reporterId), eq(pageable)
            );
        }

        @Test
        void shouldReturnEmpty_WhenNoReportsToModerate() {
            UUID reporterId = UUID.randomUUID();
            int page = 0;
            int size = 10;
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<UserReportDetailsView> pageResult = new PageImpl<>(List.of(), pageable, 0);

            when(userReportRepository.findUserReportsByStatusAndNotReporterIdWithReportedUserAndReporter(
                    eq(ReportStatus.PENDING), eq(reporterId), eq(pageable)
            )).thenReturn(pageResult);

            var result = adapter.getReportsToModerateWithReportedUserAndReporter(reporterId, size, page);

            assertNotNull(result);
            assertTrue(result.items().isEmpty());
            assertEquals(0, result.total());
        }
        
        @Test
        void shouldNotReturnReportsWhereModeratorIsTheReporter() {
            var moderatorId = UUID.randomUUID();
            int page = 0;
            int size = 10;
            var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            when(userReportRepository.findUserReportsByStatusAndNotReporterIdWithReportedUserAndReporter(
                    eq(ReportStatus.PENDING), eq(moderatorId), eq(pageable)
            )).thenReturn(new PageImpl<>(List.of(), pageable, 0));

            var result = adapter.getReportsToModerateWithReportedUserAndReporter(moderatorId, size, page);

            assertNotNull(result);
            assertTrue(result.items().isEmpty(), "Le modérateur ne doit pas voir les signalements dont il est l'auteur");
        }

        @Test
        void shouldNotReturnReportsWhereModeratorIsReportedUser() {
            var reporterId = UUID.randomUUID();
            int page = 0;
            int size = 10;
            var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            when(userReportRepository.findUserReportsByStatusAndNotReporterIdWithReportedUserAndReporter(
                    eq(ReportStatus.PENDING), eq(reporterId), eq(pageable)
            )).thenReturn(new PageImpl<>(List.of(), pageable, 0)); 

            var result = adapter.getReportsToModerateWithReportedUserAndReporter(reporterId, size, page);

            assertNotNull(result);
            assertTrue(result.items().isEmpty(), "Le modérateur ne doit pas voir les signalements où il est la personne signalée");
        }
    }

    private static UserReportDetailsView createUserReportDetailsView(UserReportDetails details) {
        return new UserReportDetailsView() {
            @Override
            public UUID getId() {
                return details.id();
            }

            @Override
            public UUID getReportedUserId() {
                return details.reportedUserId();
            }

            @Override
            public String getReportedUsername() {
                return details.reportedUsername();
            }

            @Override
            public String getReporterUsername() {
                return details.reporterUsername();
            }

            @Override
            public String getReportType() {
                return details.reportType();
            }

            @Override
            public String getJustification() {
                return details.justification();
            }

            @Override
            public Instant getCreatedAt() {
                return details.createdAt();
            }
        };
    }
}