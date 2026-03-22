package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserReportEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.UserReportDetailsView;
import fr.uge.forkeat.service.model.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserReportRepository extends JpaRepository<UserReportEntity, UUID> {

    List<UserReportEntity> findByReportedUserId(UUID reportedUserId);

    List<UserReportEntity> findByStatus(ReportStatus status);

    List<UserReportEntity> findByReporterId(UUID reporterId);

    boolean existsById(UUID userReportId);

    boolean existsByIdAndReporterId(UUID userReportId, UUID reporterId);

    boolean existsByReportedUserIdAndReporterId(UUID reportedUserId, UUID reporterId);

    long countByStatus(ReportStatus status);

    @Query("""
        SELECT r.id AS id, reported.id AS reportedUserId, reported.username AS reportedUsername,
               reporter.username AS reporterUsername, r.reportType AS reportType,
               r.justification AS justification, r.createdAt AS createdAt
        FROM UserReportEntity r
        JOIN r.reportedUser reported
        LEFT JOIN r.reporter reporter
        WHERE reporter.id <> :reporterId AND reported.id <> :reporterId AND r.status = :status
    """)
    Page<UserReportDetailsView> findUserReportsByStatusAndNotReporterIdWithReportedUserAndReporter(ReportStatus status, UUID reporterId, Pageable pageable);
}