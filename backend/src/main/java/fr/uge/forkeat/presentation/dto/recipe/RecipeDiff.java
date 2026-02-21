package fr.uge.forkeat.presentation.dto.recipe;

import java.util.*;
import java.util.stream.Collectors;

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

    // Je veux ma factory sauf que je ne peux pas mettre le constructeur en private (c'est un record)
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

    // Factory methode, RecipeDiff va être partagée entre rest et mvc,
    // On veut juste calculer la difference donc pas besion de construire
    // Le record RecipeDiff à la main
    public static RecipeDiff compute(RecipeDTO parent, RecipeDTO variant) {
        Objects.requireNonNull(parent);
        Objects.requireNonNull(variant);

        var titleChanged   = !Objects.equals(parent.title(),   variant.title());
        var summaryChanged = !Objects.equals(parent.summary(), variant.summary());
        var timeDelta      = variant.preparationMinutes() - parent.preparationMinutes();
        var imageChanged   = !Objects.equals(parent.imageUrl(), variant.imageUrl());

        return new RecipeDiff(
            titleChanged, parent.title(),
            summaryChanged, parent.summary(),
            timeDelta, imageChanged,
            List.copyOf(computeIngredientDiffs(parent, variant)),
            List.copyOf(computeStepDiffs(parent, variant)),
            List.copyOf(computeAllergenDiffs(parent, variant)),
            List.copyOf(computeDietaryFlagDiffs(parent, variant))
        );
    }

    private static List<IngredientDiff> computeIngredientDiffs(RecipeDTO parent, RecipeDTO variant) {
        var parentIngByName  = new LinkedHashMap<String, RecipeIngredientDTO>();     // Pourquoi LinkedHashMap ? Parce qu'on veut garder l'ordre d'insertion
        var variantIngByName = new LinkedHashMap<String, RecipeIngredientDTO>();
        for (var ing : parent.ingredients())  parentIngByName.putIfAbsent(ing.name().toLowerCase(Locale.ROOT), ing);
        for (var ing : variant.ingredients()) variantIngByName.putIfAbsent(ing.name().toLowerCase(Locale.ROOT), ing);

        var diffs = new ArrayList<IngredientDiff>();
        for (var varIng : variant.ingredients()) {
            var key       = varIng.name().toLowerCase(Locale.ROOT);
            var parentIng = parentIngByName.get(key);
            if (parentIng == null) {
                diffs.add(new IngredientDiff(DiffType.ADDED, varIng.name(), varIng.quantity(), varIng.unit(), 0, ""));
            } else if (parentIng.quantity() == varIng.quantity() && parentIng.unit().equals(varIng.unit())) {
                diffs.add(new IngredientDiff(DiffType.UNCHANGED, varIng.name(), varIng.quantity(), varIng.unit(), 0, ""));
            } else {
                diffs.add(new IngredientDiff(DiffType.MODIFIED, varIng.name(), varIng.quantity(), varIng.unit(), parentIng.quantity(), parentIng.unit()));
            }
        }
        for (var parentIng : parent.ingredients()) {
            if (!variantIngByName.containsKey(parentIng.name().toLowerCase(Locale.ROOT))) {
                diffs.add(new IngredientDiff(DiffType.REMOVED, parentIng.name(), parentIng.quantity(), parentIng.unit(), 0, ""));
            }
        }
        return diffs;
    }

    private static List<StepDiff> computeStepDiffs(RecipeDTO parent, RecipeDTO variant) {
        var diffs       = new ArrayList<StepDiff>();
        var parentSize  = parent.steps().size();
        var variantSize = variant.steps().size();
        var minSize     = Math.min(parentSize, variantSize);

        for (var i = 0; i < minSize; i++) {
            var ps = parent.steps().get(i);
            var vs = variant.steps().get(i);
            diffs.add(ps.instruction().equals(vs.instruction())
                ? new StepDiff(DiffType.UNCHANGED, vs.stepNumber(), vs.instruction(), "")
                : new StepDiff(DiffType.MODIFIED,  vs.stepNumber(), vs.instruction(), ps.instruction()));
        }
        for (var i = minSize; i < variantSize; i++) {
            var s = variant.steps().get(i);
            diffs.add(new StepDiff(DiffType.ADDED, s.stepNumber(), s.instruction(), ""));
        }
        for (var i = minSize; i < parentSize; i++) {
            var s = parent.steps().get(i);
            diffs.add(new StepDiff(DiffType.REMOVED, s.stepNumber(), s.instruction(), ""));
        }
        return diffs;
    }

    private static List<AllergenDiff> computeAllergenDiffs(RecipeDTO parent, RecipeDTO variant) {
        var parentKeys  = parent.allergens().stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        var variantKeys = variant.allergens().stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        var diffs = new ArrayList<AllergenDiff>();
        for (var a : variant.allergens()) {
            diffs.add(new AllergenDiff(
                parentKeys.contains(a.name().toLowerCase(Locale.ROOT)) ? DiffType.UNCHANGED : DiffType.ADDED,
                a.name(), a.severity()));
        }
        for (var a : parent.allergens()) {
            if (!variantKeys.contains(a.name().toLowerCase(Locale.ROOT))) {
                diffs.add(new AllergenDiff(DiffType.REMOVED, a.name(), a.severity()));
            }
        }
        return diffs;
    }

    private static List<DietaryFlagDiff> computeDietaryFlagDiffs(RecipeDTO parent, RecipeDTO variant) {
        Map<String, Boolean> parentFlags  = parent.dietaryFlags()  != null ? parent.dietaryFlags()  : Map.of();
        Map<String, Boolean> variantFlags = variant.dietaryFlags() != null ? variant.dietaryFlags() : Map.of();
        var allKeys = new HashSet<String>();
        allKeys.addAll(parentFlags.keySet());
        allKeys.addAll(variantFlags.keySet());

        var diffs = new ArrayList<DietaryFlagDiff>();
        for (var key : allKeys) {
            var inParent  = parentFlags.getOrDefault(key, false);
            var inVariant = variantFlags.getOrDefault(key, false);
            if      (inParent && inVariant)  diffs.add(new DietaryFlagDiff(DiffType.UNCHANGED, key));
            else if (!inParent && inVariant) diffs.add(new DietaryFlagDiff(DiffType.ADDED,     key));
            else if (inParent)               diffs.add(new DietaryFlagDiff(DiffType.REMOVED,   key));
        }
        return diffs;
    }
}
