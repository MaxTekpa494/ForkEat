package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserReportEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.UserReportDetailsView;
import fr.uge.forkeat.service.model.user.UserReport;
import fr.uge.forkeat.service.model.user.projection.UserReportDetails;

public final class UserReportEntityMapper {

    private UserReportEntityMapper() {}

    public static UserReport toDomain(UserReportEntity entity) {
        return new UserReport(
                entity.getId(),
                entity.getReportedUser().getId(),
                entity.getReporter() != null ? entity.getReporter().getId() : null,
                entity.getReportType(),
                entity.getStatus(),
                entity.getJustification(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getReviewedAt(),
                entity.getReviewedBy() != null ? entity.getReviewedBy().getId() : null
        );
    }

    public static UserReportEntity toEntity(UserReport report, UserEntity reportedUser, UserEntity reporter, UserEntity reviewedBy) {
        var entity = new UserReportEntity();
        entity.setId(report.id());
        entity.setReportedUser(reportedUser);
        entity.setReporter(reporter);
        entity.setReportType(report.reportType());
        entity.setStatus(report.status());
        entity.setJustification(report.justification());
        entity.setReviewedAt(report.reviewedAt());
        entity.setReviewedBy(reviewedBy);
        return entity;
    }

    public static UserReportDetails toDomain(UserReportDetailsView view) {
        return new UserReportDetails(
                view.getId(),
                view.getReportedUserId(),
                view.getReportedUsername(),
                view.getReporterUsername(),
                view.getReportType(),
                view.getJustification(),
                view.getCreatedAt()
        );
    }
}