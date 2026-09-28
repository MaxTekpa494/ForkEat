package fr.uge.forkeat.infrastructure.persistence.postgres.projection;

import java.time.Instant;

public interface AuthorRecipeSummaryView extends RecipeSummaryView {
    String getStatus();
    String getJustification();
    Instant getRejectedAt();
}