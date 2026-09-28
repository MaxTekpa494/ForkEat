package fr.uge.forkeat.presentation.dto.user;

import fr.uge.forkeat.service.model.ReportStatus;
import fr.uge.forkeat.service.model.user.UserReport;
import fr.uge.forkeat.service.model.user.UserReportType;

import java.time.Instant;
import java.util.UUID;

public record UserReportDTO(
        UUID id,
        UUID reportedUserId,
        UUID reporterId,
        UserReportType reportType,
        ReportStatus status,
        String justification,
        Instant createdAt,
        Instant updatedAt,
        Instant reviewedAt,
        UUID reviewedById
) {
    public static UserReportDTO from(UserReport report) {
        return new UserReportDTO(
                report.id(),
                report.reportedUserId(),
                report.reporterId(),
                report.reportType(),
                report.status(),
                report.justification(),
                report.createdAt(),
                report.updatedAt(),
                report.reviewedAt(),
                report.reviewedById()
        );
    }
}