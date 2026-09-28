package fr.uge.forkeat.service.model.user.projection;

import java.time.Instant;
import java.util.UUID;

public record UserReportDetails(
    UUID id,
    UUID reportedUserId,
    String reportedUsername,
    String reporterUsername,
    String reportType,
    String justification,
    Instant createdAt
) {}


