package fr.uge.forkeat.infrastructure.persistence.postgres.projection;

import java.time.Instant;
import java.util.UUID;

public interface RecipeBaseSummaryView {
    UUID getId();
    String getTitle();
    String getSummary();
    String getImageUrl();
    int getPreparationMinutes();
    Instant getCreatedAt();
}