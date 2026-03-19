package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.ModeratorIsAuthorException;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.user.CreateUserModerationAction;
import fr.uge.forkeat.service.model.user.UserModerationAction;
import fr.uge.forkeat.service.model.user.UserModerationActionType;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.persistence.UserModerationActionPersistence;
import fr.uge.forkeat.service.persistence.UserPersistence;
import fr.uge.forkeat.service.persistence.UserReportPersistence;
import fr.uge.forkeat.service.port.UserIdentityPort;
import fr.uge.forkeat.service.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                                       UserIdentityPort userIdentityPort,
                                       UserService userService) {
        this.moderationActionPersistence = moderationActionPersistence;
        this.userPersistence = userPersistence;
        this.userReportPersistence = userReportPersistence;
        this.userIdentityPort = userIdentityPort;
    }

    @Transactional
    public UserModerationAction moderateUser(CreateUserModerationAction command) {
        Objects.requireNonNull(command);
        if(!userPersistence.existsById(command.userId())) {
            throw new ResourceNotFoundException("User not found with id : " + command.userId());
        }
        if(!userReportPersistence.existsById(command.relatedReportId())) {
            throw new ResourceNotFoundException("User report not found with id : " + command.relatedReportId());
        }
        var moderatorId = userIdentityPort.findIdByUsernameOrThrow(command.moderatorUsername());

        if (userReportPersistence.isAuthor(command.relatedReportId(), moderatorId)) {
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
        ReportStatus reportStatus = null;
        switch(command.moderationActionType()) {
          case SUSPENDED -> {
              reportStatus = ReportStatus.VALIDATED;
              userPersistence.suspendUser(command.userId(), command.suspendedUntil());
          }
          case BANNED -> {
              reportStatus = ReportStatus.VALIDATED;
              userPersistence.banUser(command.userId());
          }
          case WARNING -> {
              reportStatus = ReportStatus.VALIDATED;
          }
          case DISMISSED -> {
              reportStatus = ReportStatus.DISMISSED;
          }
        }
        userReportPersistence.updateStatus(command.relatedReportId(), moderatorId, reportStatus);
        return moderationActionRow;
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
