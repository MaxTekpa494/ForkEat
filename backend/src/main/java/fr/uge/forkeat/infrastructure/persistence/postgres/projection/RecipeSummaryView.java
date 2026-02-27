package fr.uge.forkeat.infrastructure.persistence.postgres.projection;

import java.time.Instant;
import java.util.UUID;

public interface RecipeSummaryView extends RecipeBaseSummaryView {
    String getAuthorUsername();
}