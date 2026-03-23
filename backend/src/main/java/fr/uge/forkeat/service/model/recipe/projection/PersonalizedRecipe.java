package fr.uge.forkeat.service.model.recipe.projection;

import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeDiff;
import fr.uge.forkeat.service.model.recipe.RecipeUserInteraction;

import java.util.Objects;

public record PersonalizedRecipe(
        Recipe recipe,
        RecipeCounts counts,
        RecipeUserInteraction interaction,
        RecipeDiff diff
) {

    public PersonalizedRecipe {
        Objects.requireNonNull(recipe);
        Objects.requireNonNull(counts);
        Objects.requireNonNull(interaction);
    }

    public PersonalizedRecipe(Recipe recipe, RecipeCounts counts, RecipeUserInteraction interaction) {
        this(recipe, counts, interaction, null);
    }
}