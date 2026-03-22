package fr.uge.forkeat.infrastructure.persistence.postgres.projection;

import java.time.Instant;
import java.util.UUID;

public interface UserReportDetailsView {
    UUID getId();
    UUID getReportedUserId();
    String getReportedUsername();
    String getReporterUsername();
    String getReportType();
    String getJustification();
    Instant getCreatedAt();
}

