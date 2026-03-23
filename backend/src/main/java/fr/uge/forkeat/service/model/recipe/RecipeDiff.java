package fr.uge.forkeat.service.model.recipe;

import java.util.List;

/**
 * Resultat du calcul de difference entre une recette parente et sa variante.
 */
public record RecipeDiff(
    boolean titleChanged,
    String originalTitle,
    boolean summaryChanged,
    String originalSummary,
    int timeDelta,
    boolean imageChanged,
    List<IngredientDiff> ingredients,
    List<StepDiff> steps,
    List<AllergenDiff> allergens,
    List<DietaryFlagDiff> dietaryFlags) {

    public enum DiffType { UNCHANGED, ADDED, REMOVED, MODIFIED }

    public record IngredientDiff(
        DiffType type,
        String name,
        double quantity,
        String unit,
        double originalQuantity,
        String originalUnit) {}

    public record StepDiff(
        DiffType type,
        int stepNumber,
        String instruction,
        String originalInstruction) {}

    public record AllergenDiff(
        DiffType type,
        String name,
        String severity) {}

    public record DietaryFlagDiff(
        DiffType type,
        String flagName) {}
}
