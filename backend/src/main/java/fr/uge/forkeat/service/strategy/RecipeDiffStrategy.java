package fr.uge.forkeat.service.strategy;

import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeDiff;

@FunctionalInterface
public interface RecipeDiffStrategy {
    RecipeDiff compute(Recipe parent, Recipe variant);
}
