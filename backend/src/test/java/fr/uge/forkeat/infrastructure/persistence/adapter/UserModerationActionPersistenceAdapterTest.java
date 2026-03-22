package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserModerationActionEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserReportEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserModerationActionRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserReportRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.user.UserModerationAction;
import fr.uge.forkeat.service.model.user.UserModerationActionType;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserModerationActionPersistenceAdapterTest {

    @Mock private UserModerationActionRepository userModerationActionRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserReportRepository userReportRepository;

    private UserModerationActionPersistenceAdapter adapter;
    private UserEntity user;
    private UserEntity moderator;
    private UserReportEntity relatedReport;
    private Instant now;

    @BeforeEach
    void setUp() {
        adapter = new UserModerationActionPersistenceAdapter(userModerationActionRepository, userRepository, userReportRepository);
        now = Instant.now();

        user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername("target");
        user.setFirstName("Alice");
        user.setLastName("Martin");
        user.setEmail("target@test.com");
        user.setPassword("hashed");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.LOCAL);

        moderator = new UserEntity();
        moderator.setId(UUID.randomUUID());
        moderator.setUsername("moderator");
        moderator.setFirstName("Bob");
        moderator.setLastName("Dupont");
        moderator.setEmail("moderator@test.com");
        moderator.setPassword("hashed");
        moderator.setRole(UserRole.MODERATOR);
        moderator.setStatus(UserStatus.ACTIVE);
        moderator.setAuthMode(AuthMode.LOCAL);

        relatedReport = new UserReportEntity();
        relatedReport.setId(UUID.randomUUID());
        relatedReport.setReportedUser(user);
        relatedReport.setReporter(moderator);
        relatedReport.setReportType(UserReportType.HARASSMENT);
        relatedReport.setStatus(ReportStatus.PENDING);
        relatedReport.setJustification("Comportement abusif");
    }

    private UserModerationAction createAction(UUID relatedReportId, Instant suspendedUntil) {
        return new UserModerationAction(
                UUID.randomUUID(),
                user.getId(),
                moderator.getId(),
                UserModerationActionType.SUSPENDED,
                "Suspendu 7 jours",
                now,
                now,
                suspendedUntil,
                relatedReportId
        );
    }

    private UserModerationActionEntity createSavedEntity(UserReportEntity report, Instant suspendedUntil) {
        var entity = new UserModerationActionEntity();
        entity.setId(UUID.randomUUID());
        entity.setUser(user);
        entity.setModerator(moderator);
        entity.setModerationActionType(UserModerationActionType.SUSPENDED);
        entity.setJustification("Suspendu 7 jours");
        entity.setSuspendedUntil(suspendedUntil);
        entity.setUpdatedAt(now);
        entity.setRelatedReport(report);
        return entity;
    }

    @Nested
    class Save {

        @Test
        void shouldPersistModerationAction() {
            var suspendedUntil = now.plusSeconds(7 * 24 * 3600L);
            var action = createAction(relatedReport.getId(), suspendedUntil);
            var savedEntity = createSavedEntity(relatedReport, suspendedUntil);

            when(userRepository.getReferenceById(user.getId())).thenReturn(user);
            when(userRepository.getReferenceById(moderator.getId())).thenReturn(moderator);
            when(userReportRepository.getReferenceById(relatedReport.getId())).thenReturn(relatedReport);
            when(userModerationActionRepository.save(any())).thenReturn(savedEntity);

            var result = adapter.save(action);

            assertNotNull(result);
            assertEquals(user.getId(), result.userId());
            assertEquals(moderator.getId(), result.moderatorId());
            assertEquals(UserModerationActionType.SUSPENDED, result.moderationActionType());
            assertEquals(relatedReport.getId(), result.relatedReportId());
            assertEquals(suspendedUntil, result.suspendedUntil());
            verify(userModerationActionRepository).save(any());
        }

        @Test
        void shouldThrowWhenActionIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.save(null));
        }
    }

    @Nested
    class FindByUserId {

        @Test
        void shouldReturnModerationActions() {
            var entity = createSavedEntity(relatedReport, now.plusSeconds(3600));
            when(userModerationActionRepository.findByUserId(user.getId())).thenReturn(List.of(entity));

            var result = adapter.findByUserId(user.getId());

            assertEquals(1, result.size());
            assertEquals(user.getId(), result.getFirst().userId());
            verify(userModerationActionRepository).findByUserId(user.getId());
        }

        @Test
        void shouldReturnEmptyListWhenNoAction() {
            when(userModerationActionRepository.findByUserId(user.getId())).thenReturn(List.of());

            var result = adapter.findByUserId(user.getId());

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldThrowWhenUserIdIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.findByUserId(null));
        }
    }

    @Nested
    class FindByType {

        @Test
        void shouldReturnMatchingActions() {
            var entity = createSavedEntity(relatedReport, null);
            entity.setModerationActionType(UserModerationActionType.WARNING);
            when(userModerationActionRepository.findByModerationActionType(UserModerationActionType.WARNING))
                    .thenReturn(List.of(entity));

            var result = adapter.findByActionType(UserModerationActionType.WARNING);

            assertEquals(1, result.size());
            assertEquals(UserModerationActionType.WARNING, result.getFirst().moderationActionType());
            verify(userModerationActionRepository).findByModerationActionType(UserModerationActionType.WARNING);
        }
    }
}

