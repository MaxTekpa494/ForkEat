package fr.uge.forkeat.service.model.user;

import fr.uge.forkeat.service.model.ReportStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UserReport(
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
    public UserReport {
        Objects.requireNonNull(id);
        Objects.requireNonNull(reportedUserId);
        Objects.requireNonNull(reportType);
        Objects.requireNonNull(status);
        Objects.requireNonNull(justification);
        if (justification.isBlank()) {
            throw new IllegalArgumentException("justification cannot be empty");
        }
    }
}