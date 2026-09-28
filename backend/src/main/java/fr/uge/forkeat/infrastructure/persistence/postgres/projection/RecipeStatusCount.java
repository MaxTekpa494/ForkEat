package fr.uge.forkeat.infrastructure.persistence.postgres.projection;

public interface RecipeStatusCount {
    String getStatus();
    long getCount();
}