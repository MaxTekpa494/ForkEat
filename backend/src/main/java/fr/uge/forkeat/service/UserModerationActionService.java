package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.ModeratorIsAuthorException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.user.CreateUserModerationAction;
import fr.uge.forkeat.service.model.user.UserModerationAction;
import fr.uge.forkeat.service.model.user.UserModerationActionType;
import fr.uge.forkeat.service.model.user.UserReport;
import fr.uge.forkeat.service.persistence.UserModerationActionPersistence;
import fr.uge.forkeat.service.persistence.UserPersistence;
import fr.uge.forkeat.service.persistence.UserReportPersistence;
import fr.uge.forkeat.service.port.UserIdentityPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserModerationActionService {
    private final UserModerationActionPersistence moderationActionPersistence;
    private final UserPersistence userPersistence;
    private final UserReportPersistence userReportPersistence;
    private final UserIdentityPort userIdentityPort;

    public UserModerationActionService(UserModerationActionPersistence moderationActionPersistence,
                                       UserPersistence userPersistence,
                                       UserReportPersistence userReportPersistence,
                                       UserIdentityPort userIdentityPort) {
        this.moderationActionPersistence = moderationActionPersistence;
        this.userPersistence = userPersistence;
        this.userReportPersistence = userReportPersistence;
        this.userIdentityPort = userIdentityPort;
    }

    @Transactional
    public UserModerationAction moderateUser(CreateUserModerationAction command) {
        Objects.requireNonNull(command);
        if (command.suspendedUntil() != null && !command.suspendedUntil().isAfter(Instant.now())) {
            throw new IllegalArgumentException("suspendedUntil must be in the future");
        }
        if(!userPersistence.existsById(command.userId())) {
            throw new ResourceNotFoundException("User not found with id : " + command.userId());
        }
        if(!userReportPersistence.existsById(command.relatedReportId())) {
            throw new ResourceNotFoundException("User report not found with id : " + command.relatedReportId());
        }
        var moderatorId = userIdentityPort.findIdByUsernameOrThrow(command.moderatorUsername());

        if (userReportPersistence.isAuthor(command.relatedReportId(), moderatorId)) { // Moderator created the report
            throw new ModeratorIsAuthorException();
        }
        if(moderatorId.equals(command.userId())) { // Moderator is the signaled user
            throw new ModeratorIsAuthorException();
        }

        var moderationAction = new UserModerationAction(
                UUID.randomUUID(),
                command.userId(),
                moderatorId,
                command.moderationActionType(),
                command.justification(),
                null,
                null,
                command.suspendedUntil(),
                command.relatedReportId()
        );
        var moderationActionRow = moderationActionPersistence.save(moderationAction);

        var reportStatus = handleUserModeration(command);
        updateReportStatus(command.relatedReportId(), moderatorId, reportStatus);
        return moderationActionRow;
    }

    private ReportStatus handleUserModeration(CreateUserModerationAction command) {
        return switch(command.moderationActionType()) {
            case SUSPENDED -> {
                var user = userPersistence.findById(command.userId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with id : " + command.userId()));
                var suspendedUser = user.suspend();
                userPersistence.updateUser(suspendedUser);
                yield ReportStatus.VALIDATED;
            }
            case BANNED -> {
                var user = userPersistence.findById(command.userId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with id : " + command.userId()));
                var bannedUser = user.ban();
                userPersistence.updateUser(bannedUser);
                yield ReportStatus.VALIDATED;
            }
            case WARNING -> ReportStatus.VALIDATED;
            case DISMISSED -> ReportStatus.DISMISSED;
        };
    }

    private void updateReportStatus(UUID reportId, UUID reviewerId, ReportStatus status) {
        var report = userReportPersistence.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException("User report not found with id : " + reportId));
        var updated = new UserReport(
            report.id(),
            report.reportedUserId(),
            report.reporterId(),
            report.reportType(),
            status,
            report.justification(),
            report.createdAt(),
            report.updatedAt(),
            Instant.now(),
            reviewerId
        );
        userReportPersistence.save(updated);
    }

    public List<UserModerationAction> findByType(UserModerationActionType type) {
        Objects.requireNonNull(type);
        return moderationActionPersistence.findByActionType(type);
    }

    public List<UserModerationAction> findByUserId(UUID userId) {
        Objects.requireNonNull(userId);
        if (!userPersistence.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id : " + userId);
        }
        return moderationActionPersistence.findByUserId(userId);
    }
}
