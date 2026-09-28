package fr.uge.forkeat.service;

import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeDiff;
import fr.uge.forkeat.service.strategy.RecipeDiffStrategy;
import org.springframework.stereotype.Service;

import java.util.Objects;


@Service
public class RecipeDiffService {

    private final RecipeDiffStrategy strategy;

    public RecipeDiffService(RecipeDiffStrategy strategy) {
        this.strategy = strategy;
    }

    public RecipeDiff computeDiff(Recipe parent, Recipe variant) {
        Objects.requireNonNull(parent);
        Objects.requireNonNull(variant);
        return strategy.compute(parent, variant);
    }
}
