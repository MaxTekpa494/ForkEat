package fr.uge.forkeat.service.model.recipe.projection;

import fr.uge.forkeat.service.model.recipe.RecipeUserInteraction;

import java.util.Objects;

public record PersonalizedRecipeSummary(
        RecipeSummary summary,
        RecipeCounts counts,
        RecipeUserInteraction interaction
) {

    public PersonalizedRecipeSummary {
        Objects.requireNonNull(summary);
        Objects.requireNonNull(counts);
        Objects.requireNonNull(interaction);
    }
}