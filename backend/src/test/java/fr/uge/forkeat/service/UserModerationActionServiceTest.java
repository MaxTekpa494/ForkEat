package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.user.*;
import fr.uge.forkeat.service.persistence.UserModerationActionPersistence;
import fr.uge.forkeat.service.persistence.UserPersistence;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserModerationActionServiceTest {

    @Mock
    private UserModerationActionPersistence moderationActionPersistence;
    @Mock
    private UserPersistence userPersistence;
    @Mock
    private UserReportPersistence userReportPersistence;
    @Mock
    private UserIdentityPort userIdentityPort;

    private UserModerationActionService service;

    @BeforeEach
    void setUp() {
        service = new UserModerationActionService(
                moderationActionPersistence,
                userPersistence,
                userReportPersistence,
                userIdentityPort
        );
    }

    private CreateUserModerationAction createCommand(
            UUID userId,
            String moderatorUsername,
            UserModerationActionType type,
            Instant suspendedUntil,
            UUID relatedReportId
    ) {
        return new CreateUserModerationAction(
                userId,
                moderatorUsername,
                type,
                "Justification",
                suspendedUntil,
                relatedReportId
        );
    }

    private UserModerationAction createModerationAction(
            UUID userId,
            UUID moderatorId,
            UserModerationActionType type,
            Instant suspendedUntil,
            UUID relatedReportId
    ) {
        return new UserModerationAction(
                UUID.randomUUID(),
                userId,
                moderatorId,
                type,
                "Justification",
                Instant.now(),
                Instant.now(),
                suspendedUntil,
                relatedReportId
        );
    }

    private User createUser(UUID id) {
        return new User(
            id,
            "username",
            "first",
            "last",
            "user@email.com",
            UserRole.MEMBER,
            UserStatus.ACTIVE,
            AuthMode.LOCAL,
            Instant.now(),
            Instant.now(),
            true
        );
    }

    @Nested
    class ModerateUser {

        @Test
        void shouldSuspendUserAndValidateReport_WhenActionIsSuspended() {
            var userId = UUID.randomUUID();
            var reportId = UUID.randomUUID();
            var moderatorId = UUID.randomUUID();
            var suspendedUntil = Instant.now().plusSeconds(3600);
            var command = createCommand(userId, "mod", UserModerationActionType.SUSPENDED, suspendedUntil, reportId);
            var expected = createModerationAction(userId, moderatorId, UserModerationActionType.SUSPENDED, suspendedUntil, reportId);
            var report = createUserReport(reportId, userId);
            var user = createUser(userId);
            when(userPersistence.existsById(userId)).thenReturn(true);
            when(userPersistence.findById(userId)).thenReturn(Optional.of(user));
            when(userReportPersistence.existsById(reportId)).thenReturn(true);
            when(userReportPersistence.findById(reportId)).thenReturn(Optional.of(report));
            when(userIdentityPort.findIdByUsernameOrThrow("mod")).thenReturn(moderatorId);
            when(userReportPersistence.isAuthor(reportId, moderatorId)).thenReturn(false);
            when(moderationActionPersistence.save(any())).thenReturn(expected);

            var result = service.moderateUser(command);

            assertNotNull(result);
            assertEquals(userId, result.userId());
            assertEquals(UserModerationActionType.SUSPENDED, result.moderationActionType());
            verify(moderationActionPersistence).save(argThat(action ->
                    action.userId().equals(userId)
                            && action.moderatorId().equals(moderatorId)
                            && action.moderationActionType() == UserModerationActionType.SUSPENDED
                            && action.suspendedUntil().equals(suspendedUntil)
                            && action.relatedReportId().equals(reportId)
            ));
            verify(userPersistence).updateUser(argThat(u ->
                u.id().equals(userId)
                && u.status() == UserStatus.SUSPENDED
            ));
            verify(userPersistence, never()).updateUser(argThat(u -> u.status() == UserStatus.BANNED));
            verify(userReportPersistence).findById(reportId);
            verify(userReportPersistence).save(argThat(saved ->
                saved.id().equals(reportId)
                && saved.status() == ReportStatus.VALIDATED
                && saved.reviewedById().equals(moderatorId)
                && saved.reviewedAt() != null
            ));
        }

        @Test
        void shouldBanUserAndValidateReport_WhenActionIsBanned() {
            var userId = UUID.randomUUID();
            var reportId = UUID.randomUUID();
            var moderatorId = UUID.randomUUID();
            var command = createCommand(userId, "mod", UserModerationActionType.BANNED, null, reportId);
            var expected = createModerationAction(userId, moderatorId, UserModerationActionType.BANNED, null, reportId);
            var report = createUserReport(reportId, userId);
            var user = createUser(userId);
            when(userPersistence.existsById(userId)).thenReturn(true);
            when(userPersistence.findById(userId)).thenReturn(Optional.of(user));
            when(userReportPersistence.existsById(reportId)).thenReturn(true);
            when(userReportPersistence.findById(reportId)).thenReturn(Optional.of(report));
            when(userIdentityPort.findIdByUsernameOrThrow("mod")).thenReturn(moderatorId);
            when(userReportPersistence.isAuthor(reportId, moderatorId)).thenReturn(false);
            when(moderationActionPersistence.save(any())).thenReturn(expected);

            var result = service.moderateUser(command);

            assertNotNull(result);
            assertEquals(UserModerationActionType.BANNED, result.moderationActionType());
            verify(userPersistence).updateUser(argThat(u ->
                u.id().equals(userId)
                && u.status() == UserStatus.BANNED
            ));
            verify(userPersistence, never()).updateUser(argThat(u -> u.status() == UserStatus.SUSPENDED));
            verify(userReportPersistence).findById(reportId);
            verify(userReportPersistence).save(argThat(saved ->
                saved.id().equals(reportId)
                && saved.status() == ReportStatus.VALIDATED
                && saved.reviewedById().equals(moderatorId)
                && saved.reviewedAt() != null
            ));
        }

        @Test
        void shouldValidateReportOnly_WhenActionIsWarning() {
            var userId = UUID.randomUUID();
            var reportId = UUID.randomUUID();
            var moderatorId = UUID.randomUUID();
            var command = createCommand(userId, "mod", UserModerationActionType.WARNING, null, reportId);
            var expected = createModerationAction(userId, moderatorId, UserModerationActionType.WARNING, null, reportId);
            var report = createUserReport(reportId, userId);
            when(userPersistence.existsById(userId)).thenReturn(true);
            when(userReportPersistence.existsById(reportId)).thenReturn(true);
            when(userReportPersistence.findById(reportId)).thenReturn(Optional.of(report));
            when(userIdentityPort.findIdByUsernameOrThrow("mod")).thenReturn(moderatorId);
            when(userReportPersistence.isAuthor(reportId, moderatorId)).thenReturn(false);
            when(moderationActionPersistence.save(any())).thenReturn(expected);

            var result = service.moderateUser(command);

            assertNotNull(result);
            assertEquals(UserModerationActionType.WARNING, result.moderationActionType());
            verify(userPersistence, never()).updateUser(any());
            verify(userReportPersistence).findById(reportId);
            verify(userReportPersistence).save(argThat(saved ->
                saved.id().equals(reportId)
                && saved.status() == ReportStatus.VALIDATED
                && saved.reviewedById().equals(moderatorId)
                && saved.reviewedAt() != null
            ));
        }

        @Test
        void shouldDismissReport_WhenActionIsDismissed() {
            var userId = UUID.randomUUID();
            var reportId = UUID.randomUUID();
            var moderatorId = UUID.randomUUID();
            var command = createCommand(userId, "mod", UserModerationActionType.DISMISSED, null, reportId);
            var expected = createModerationAction(userId, moderatorId, UserModerationActionType.DISMISSED, null, reportId);
            var report = createUserReport(reportId, userId);
            when(userPersistence.existsById(userId)).thenReturn(true);
            when(userReportPersistence.existsById(reportId)).thenReturn(true);
            when(userReportPersistence.findById(reportId)).thenReturn(Optional.of(report));
            when(userIdentityPort.findIdByUsernameOrThrow("mod")).thenReturn(moderatorId);
            when(userReportPersistence.isAuthor(reportId, moderatorId)).thenReturn(false);
            when(moderationActionPersistence.save(any())).thenReturn(expected);

            var result = service.moderateUser(command);

            assertNotNull(result);
            assertEquals(UserModerationActionType.DISMISSED, result.moderationActionType());
            verify(userPersistence, never()).updateUser(any());
            verify(userReportPersistence).findById(reportId);
            verify(userReportPersistence).save(argThat(saved ->
                saved.id().equals(reportId)
                && saved.status() == ReportStatus.DISMISSED
                && saved.reviewedById().equals(moderatorId)
                && saved.reviewedAt() != null
            ));
        }

        @Test
        void shouldThrowResourceNotFoundException_WhenReportNotFoundOnFindById() {
            var userId = UUID.randomUUID();
            var reportId = UUID.randomUUID();
            var moderatorId = UUID.randomUUID();
            var command = createCommand(userId, "mod", UserModerationActionType.WARNING, null, reportId);
            when(userPersistence.existsById(userId)).thenReturn(true);
            when(userReportPersistence.existsById(reportId)).thenReturn(true);
            when(userReportPersistence.findById(reportId)).thenReturn(Optional.empty());
            when(userIdentityPort.findIdByUsernameOrThrow("mod")).thenReturn(moderatorId);
            when(userReportPersistence.isAuthor(reportId, moderatorId)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class, () -> service.moderateUser(command));
            verify(userReportPersistence).findById(reportId);
            verify(userReportPersistence, never()).save(any());
        }

        @Test
        void shouldThrowException_WhenSuspendedUntilNotAfterReportDate() {
            var userId = UUID.randomUUID();
            var reportId = UUID.randomUUID();
            var reportCreatedAt = Instant.now();
            var suspendedUntil = reportCreatedAt.minusSeconds(10); // avant la date du rapport
            var command = createCommand(userId, "mod", UserModerationActionType.SUSPENDED, suspendedUntil, reportId);
            assertThrows(IllegalArgumentException.class, () -> service.moderateUser(command));
        }

        @Test
        void shouldThrowResourceNotFoundException_WhenUserNotFound() {
            var userId = UUID.randomUUID();
            var reportId = UUID.randomUUID();
            var command = createCommand(userId, "mod", UserModerationActionType.BANNED, null, reportId);
            when(userPersistence.existsById(userId)).thenReturn(true);
            assertThrows(ResourceNotFoundException.class, () -> service.moderateUser(command));
        }

        private UserReport createUserReport(UUID reportId, UUID reportedUserId) {
            return new UserReport(
                reportId,
                reportedUserId,
                UUID.randomUUID(),
                fr.uge.forkeat.service.model.user.UserReportType.SPAM,
                ReportStatus.PENDING,
                "Justification",
                Instant.now(),
                Instant.now(),
                null,
                null
            );
        }
    }

    @Nested
    class FindByType {

        @Test
        void shouldReturnActionsByType() {
            var action = createModerationAction(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UserModerationActionType.WARNING,
                    null,
                    UUID.randomUUID()
            );

            when(moderationActionPersistence.findByActionType(UserModerationActionType.WARNING))
                    .thenReturn(List.of(action));

            var result = service.findByType(UserModerationActionType.WARNING);

            assertEquals(1, result.size());
            assertEquals(UserModerationActionType.WARNING, result.getFirst().moderationActionType());
            verify(moderationActionPersistence).findByActionType(UserModerationActionType.WARNING);
        }

        @Test
        void shouldThrowNullPointerException_WhenTypeIsNull() {
            assertThrows(NullPointerException.class, () -> service.findByType(null));
            verifyNoInteractions(moderationActionPersistence);
        }
    }

    @Nested
    class FindByUserId {

        @Test
        void shouldReturnActions_WhenUserExists() {
            var userId = UUID.randomUUID();
            var actions = List.of(createModerationAction(
                    userId,
                    UUID.randomUUID(),
                    UserModerationActionType.BANNED,
                    null,
                    UUID.randomUUID()
            ));

            when(userPersistence.existsById(userId)).thenReturn(true);
            when(moderationActionPersistence.findByUserId(userId)).thenReturn(actions);

            var result = service.findByUserId(userId);

            assertEquals(1, result.size());
            assertEquals(userId, result.getFirst().userId());
            verify(userPersistence).existsById(userId);
            verify(moderationActionPersistence).findByUserId(userId);
        }

        @Test
        void shouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
            var userId = UUID.randomUUID();
            when(userPersistence.existsById(userId)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class, () -> service.findByUserId(userId));
            verify(userPersistence).existsById(userId);
            verify(moderationActionPersistence, never()).findByUserId(any());
        }

        @Test
        void shouldThrowNullPointerException_WhenUserIdIsNull() {
            assertThrows(NullPointerException.class, () -> service.findByUserId(null));
            verifyNoInteractions(moderationActionPersistence, userPersistence);
        }
    }
}

