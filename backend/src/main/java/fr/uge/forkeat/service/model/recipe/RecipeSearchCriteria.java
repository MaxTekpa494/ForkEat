package fr.uge.forkeat.service.model.recipe;

import java.util.List;
import java.util.Objects;

public record RecipeSearchCriteria(
        RecipeStatus status,
        String search,
        List<String> allergens,
        int size,
        int page
) {
    public RecipeSearchCriteria {
        Objects.requireNonNull(status);
        if (allergens == null) {
            allergens = List.of();
        } else {
            allergens = List.copyOf(allergens);
        }
        if (size <= 0 || page < 0) {
            throw new IllegalArgumentException("Invalid page or size");
        }
    }
}