package fr.uge.forkeat.service.persistence;

import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.user.UserReport;
import fr.uge.forkeat.service.model.user.projection.UserReportDetails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserReportPersistence {

    UserReport save(UserReport report);

    Optional<UserReport> findById(UUID userReportId);

    List<UserReport> findByReportedUserId(UUID reportedUserId);

    List<UserReport> findByStatus(ReportStatus status);

    PageResult<UserReportDetails> getReportsToModerateWithReportedUserAndReporter(UUID reporterId, int size, int page);

    boolean existsById(UUID userReportId);

    boolean isAuthor(UUID userReportId, UUID reporterId);

    boolean existsByReportedUserIdAndReporterId(UUID reportedUserId, UUID reporterId);
}