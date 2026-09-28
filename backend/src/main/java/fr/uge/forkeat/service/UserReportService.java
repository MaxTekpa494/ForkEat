package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.UserAlreadyReportedException;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.user.CreateUserReport;
import fr.uge.forkeat.service.model.user.UserReport;
import fr.uge.forkeat.service.model.user.projection.UserReportDetails;
import fr.uge.forkeat.service.persistence.UserReportPersistence;
import fr.uge.forkeat.service.port.UserIdentityPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserReportService {

    private final UserReportPersistence userReportPersistence;
    private final UserIdentityPort userIdentityPort;

    public UserReportService(UserReportPersistence userReportPersistence,
                             UserIdentityPort userIdentityPort) {
        this.userReportPersistence = userReportPersistence;
        this.userIdentityPort = userIdentityPort;
    }

    @Transactional
    public UserReport reportUser(CreateUserReport command) {
        Objects.requireNonNull(command);
        var reportedUserId = userIdentityPort.findIdByUsernameOrThrow(command.reportedUserUsername());
        var reporterId = userIdentityPort.findIdByUsernameOrThrow(command.reporterUsername());
        if (userReportPersistence.existsByReportedUserIdAndReporterId(reportedUserId, reporterId)) {
            throw new UserAlreadyReportedException(reportedUserId, reporterId);
        }
        var report = new UserReport(
                UUID.randomUUID(),
                reportedUserId,
                reporterId,
                command.reportType(),
                ReportStatus.PENDING,
                command.justification(),
                null,
                null,
                null,
                null
        );
        return userReportPersistence.save(report);
    }

    public List<UserReport> findByStatus(ReportStatus status) {
        Objects.requireNonNull(status);
        return userReportPersistence.findByStatus(status);
    }

    public List<UserReport> findByReportedUserId(UUID reportedUserId) {
        Objects.requireNonNull(reportedUserId);
        return userReportPersistence.findByReportedUserId(reportedUserId);
    }

    public PageResult<UserReportDetails> getReportsToModerate(String reporterUsername, int size, int page) {
        Objects.requireNonNull(reporterUsername);
        var reporterId = userIdentityPort.findIdByUsernameOrThrow(reporterUsername);
        if (size <= 0 || page < 0) {
            throw new IllegalArgumentException("Invalid page or size");
        }
        return userReportPersistence.getReportsToModerateWithReportedUserAndReporter(reporterId, size, page);
    }
}