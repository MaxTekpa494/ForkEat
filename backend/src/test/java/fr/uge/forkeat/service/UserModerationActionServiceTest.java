package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.ModeratorIsAuthorException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.user.CreateUserModerationAction;
import fr.uge.forkeat.service.model.user.UserModerationAction;
import fr.uge.forkeat.service.model.user.UserModerationActionType;
import fr.uge.forkeat.service.persistence.UserModerationActionPersistence;
import fr.uge.forkeat.service.persistence.UserPersistence;
import fr.uge.forkeat.service.persistence.UserReportPersistence;
import fr.uge.forkeat.service.port.UserIdentityPort;
import fr.uge.forkeat.service.user.UserService;
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
    @Mock
    private UserService userService;

    private UserModerationActionService service;

    @BeforeEach
    void setUp() {
        service = new UserModerationActionService(
                moderationActionPersistence,
                userPersistence,
                userReportPersistence,
                userIdentityPort,
                userService
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

            when(userPersistence.existsById(userId)).thenReturn(true);
            when(userReportPersistence.existsById(reportId)).thenReturn(true);
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
            verify(userPersistence).suspendUser(userId, suspendedUntil);
            verify(userPersistence, never()).banUser(any());
            verify(userReportPersistence).updateStatus(reportId, moderatorId, ReportStatus.VALIDATED);
        }

        @Test
        void shouldBanUserAndValidateReport_WhenActionIsBanned() {
            var userId = UUID.randomUUID();
            var reportId = UUID.randomUUID();
            var moderatorId = UUID.randomUUID();
            var command = createCommand(userId, "mod", UserModerationActionType.BANNED, null, reportId);
            var expected = createModerationAction(userId, moderatorId, UserModerationActionType.BANNED, null, reportId);

            when(userPersistence.existsById(userId)).thenReturn(true);
            when(userReportPersistence.existsById(reportId)).thenReturn(true);
            when(userIdentityPort.findIdByUsernameOrThrow("mod")).thenReturn(moderatorId);
            when(userReportPersistence.isAuthor(reportId, moderatorId)).thenReturn(false);
            when(moderationActionPersistence.save(any())).thenReturn(expected);

            var result = service.moderateUser(command);

            assertNotNull(result);
            assertEquals(UserModerationActionType.BANNED, result.moderationActionType());
            verify(userPersistence).banUser(userId);
            verify(userPersistence, never()).suspendUser(any(), any());
            verify(userReportPersistence).updateStatus(reportId, moderatorId, ReportStatus.VALIDATED);
        }

        @Test
        void shouldValidateReportOnly_WhenActionIsWarning() {
            var userId = UUID.randomUUID();
            var reportId = UUID.randomUUID();
            var moderatorId = UUID.randomUUID();
            var command = createCommand(userId, "mod", UserModerationActionType.WARNING, null, reportId);
            var expected = createModerationAction(userId, moderatorId, UserModerationActionType.WARNING, null, reportId);

            when(userPersistence.existsById(userId)).thenReturn(true);
            when(userReportPersistence.existsById(reportId)).thenReturn(true);
            when(userIdentityPort.findIdByUsernameOrThrow("mod")).thenReturn(moderatorId);
            when(userReportPersistence.isAuthor(reportId, moderatorId)).thenReturn(false);
            when(moderationActionPersistence.save(any())).thenReturn(expected);

            var result = service.moderateUser(command);

            assertNotNull(result);
            assertEquals(UserModerationActionType.WARNING, result.moderationActionType());
            verify(userPersistence, never()).banUser(any());
            verify(userPersistence, never()).suspendUser(any(), any());
            verify(userReportPersistence).updateStatus(reportId, moderatorId, ReportStatus.VALIDATED);
        }

        @Test
        void shouldDismissReport_WhenActionIsDismissed() {
            var userId = UUID.randomUUID();
            var reportId = UUID.randomUUID();
            var moderatorId = UUID.randomUUID();
            var command = createCommand(userId, "mod", UserModerationActionType.DISMISSED, null, reportId);
            var expected = createModerationAction(userId, moderatorId, UserModerationActionType.DISMISSED, null, reportId);

            when(userPersistence.existsById(userId)).thenReturn(true);
            when(userReportPersistence.existsById(reportId)).thenReturn(true);
            when(userIdentityPort.findIdByUsernameOrThrow("mod")).thenReturn(moderatorId);
            when(userReportPersistence.isAuthor(reportId, moderatorId)).thenReturn(false);
            when(moderationActionPersistence.save(any())).thenReturn(expected);

            var result = service.moderateUser(command);

            assertNotNull(result);
            assertEquals(UserModerationActionType.DISMISSED, result.moderationActionType());
            verify(userPersistence, never()).banUser(any());
            verify(userPersistence, never()).suspendUser(any(), any());
            verify(userReportPersistence).updateStatus(reportId, moderatorId, ReportStatus.DISMISSED);
        }

        @Test
        void shouldThrowNullPointerException_WhenCommandIsNull() {
            assertThrows(NullPointerException.class, () -> service.moderateUser(null));
            verifyNoInteractions(moderationActionPersistence, userPersistence, userReportPersistence, userIdentityPort);
        }

        @Test
        void shouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
            var userId = UUID.randomUUID();
            var command = createCommand(userId, "mod", UserModerationActionType.WARNING, null, UUID.randomUUID());

            when(userPersistence.existsById(userId)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class, () -> service.moderateUser(command));
            verify(moderationActionPersistence, never()).save(any());
            verify(userReportPersistence, never()).updateStatus(any(), any(), any());
        }

        @Test
        void shouldThrowResourceNotFoundException_WhenReportDoesNotExist() {
            var userId = UUID.randomUUID();
            var reportId = UUID.randomUUID();
            var command = createCommand(userId, "mod", UserModerationActionType.WARNING, null, reportId);

            when(userPersistence.existsById(userId)).thenReturn(true);
            when(userReportPersistence.existsById(reportId)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class, () -> service.moderateUser(command));
            verify(moderationActionPersistence, never()).save(any());
            verify(userReportPersistence, never()).updateStatus(any(), any(), any());
        }

        @Test
        void shouldThrowModeratorIsAuthorException_WhenModeratorIsAuthorOfReport() {
            var userId = UUID.randomUUID();
            var reportId = UUID.randomUUID();
            var moderatorId = UUID.randomUUID();
            var command = createCommand(userId, "mod", UserModerationActionType.WARNING, null, reportId);

            when(userPersistence.existsById(userId)).thenReturn(true);
            when(userReportPersistence.existsById(reportId)).thenReturn(true);
            when(userIdentityPort.findIdByUsernameOrThrow("mod")).thenReturn(moderatorId);
            when(userReportPersistence.isAuthor(reportId, moderatorId)).thenReturn(true);

            assertThrows(ModeratorIsAuthorException.class, () -> service.moderateUser(command));
            verify(moderationActionPersistence, never()).save(any());
            verify(userReportPersistence, never()).updateStatus(any(), any(), any());
            verify(userPersistence, never()).banUser(any());
            verify(userPersistence, never()).suspendUser(any(), any());
        }

        @Test
        void shouldPropagateResourceNotFoundException_WhenModeratorDoesNotExist() {
            var userId = UUID.randomUUID();
            var reportId = UUID.randomUUID();
            var command = createCommand(userId, "missing-mod", UserModerationActionType.WARNING, null, reportId);

            when(userPersistence.existsById(userId)).thenReturn(true);
            when(userReportPersistence.existsById(reportId)).thenReturn(true);
            when(userIdentityPort.findIdByUsernameOrThrow("missing-mod"))
                    .thenThrow(new ResourceNotFoundException("User not found: missing-mod"));

            assertThrows(ResourceNotFoundException.class, () -> service.moderateUser(command));
            verify(moderationActionPersistence, never()).save(any());
            verify(userReportPersistence, never()).updateStatus(any(), any(), any());
            verify(userPersistence, never()).banUser(any());
            verify(userPersistence, never()).suspendUser(any(), any());
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

