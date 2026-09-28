package fr.uge.forkeat.infrastructure.persistence.postgres.projection;

public interface RecipeSummaryView extends RecipeBaseSummaryView {
    String getAuthorUsername();
}