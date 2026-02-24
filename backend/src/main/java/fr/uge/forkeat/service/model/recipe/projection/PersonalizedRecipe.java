package fr.uge.forkeat.service.model.recipe.projection;

import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeUserInteraction;

import java.util.Objects;

public record PersonalizedRecipe(
        Recipe recipe,
        RecipeCounts counts,
        RecipeUserInteraction interaction
) {

    public PersonalizedRecipe {
        Objects.requireNonNull(recipe);
        Objects.requireNonNull(counts);
        Objects.requireNonNull(interaction);
    }
}