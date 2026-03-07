package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.user.UserReport;

import java.util.List;
import java.util.UUID;

public interface UserReportPersistence {

    UserReport save(UserReport report);

    List<UserReport> findByReportedUserId(UUID reportedUserId);

    List<UserReport> findByStatus(ReportStatus status);

    boolean existsByReportedUserIdAndReporterId(UUID reportedUserId, UUID reporterId);
}