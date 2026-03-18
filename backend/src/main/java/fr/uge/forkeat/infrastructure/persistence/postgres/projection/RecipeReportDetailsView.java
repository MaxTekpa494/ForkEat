package fr.uge.forkeat.infrastructure.persistence.postgres.projection;

import java.time.Instant;
import java.util.UUID;

public interface RecipeReportDetailsView {
    UUID getId();
    UUID getRecipeId();
    String getRecipeTitle();
    String getRecipeImageUrl();
    String getReporterUsername();
    String getReportType();
    String getJustification();
    Instant getCreatedAt();
}

