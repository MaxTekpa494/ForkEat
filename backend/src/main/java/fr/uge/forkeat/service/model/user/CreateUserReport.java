package fr.uge.forkeat.service.model.user;

import java.util.Objects;

public record CreateUserReport(
        String reportedUserUsername,
        String reporterUsername,
        UserReportType reportType,
        String justification
) {
    public CreateUserReport {
        Objects.requireNonNull(reportedUserUsername);
        Objects.requireNonNull(reporterUsername);
        Objects.requireNonNull(reportType);
        Objects.requireNonNull(justification);
        if (justification.isBlank()) {
            throw new IllegalArgumentException("justification cannot be empty");
        }
    }
}