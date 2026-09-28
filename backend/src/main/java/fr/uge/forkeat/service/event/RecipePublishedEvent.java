package fr.uge.forkeat.service.event;

import java.util.Objects;
import java.util.UUID;

public record RecipePublishedEvent(UUID recipeId) implements DomainEvent{
  public RecipePublishedEvent {
    Objects.requireNonNull(recipeId, "🥶oh joie");
  }
}
