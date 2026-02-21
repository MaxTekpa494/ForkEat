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
        var     timeDelta      = variant.preparationMinutes() - parent.preparationMinutes();
        var imageChanged   = !Objects.equals(parent.imageUrl(), variant.imageUrl());

        var parentIngByName  = new LinkedHashMap<String, RecipeIngredientDTO>(); // Pourquoi LinkedHashMap ?? Parce qu'on veut garder l'ordre; même si c'est long c'est pas grave (on a pas le choix)
        var variantIngByName = new LinkedHashMap<String, RecipeIngredientDTO>();
        for (var ing : parent.ingredients())  parentIngByName.putIfAbsent(ing.name().toLowerCase(Locale.ROOT), ing);
        for (var ing : variant.ingredients()) variantIngByName.putIfAbsent(ing.name().toLowerCase(Locale.ROOT), ing);

        var ingDiffs = new ArrayList<IngredientDiff>();
        for (var varIng : variant.ingredients()) {
            var key = varIng.name().toLowerCase(Locale.ROOT);
            var parentIng = parentIngByName.get(key);
            if (parentIng == null) {
                ingDiffs.add(new IngredientDiff(DiffType.ADDED, varIng.name(), varIng.quantity(), varIng.unit(), 0, ""));
            } else if (parentIng.quantity() == varIng.quantity() && parentIng.unit().equals(varIng.unit())) {
                ingDiffs.add(new IngredientDiff(DiffType.UNCHANGED, varIng.name(), varIng.quantity(), varIng.unit(), 0, ""));
            } else {
                ingDiffs.add(new IngredientDiff(DiffType.MODIFIED, varIng.name(), varIng.quantity(), varIng.unit(), parentIng.quantity(), parentIng.unit()));
            }
        }
        for (var parentIng : parent.ingredients()) {
            if (!variantIngByName.containsKey(parentIng.name().toLowerCase(Locale.ROOT))) {
                ingDiffs.add(new IngredientDiff(DiffType.REMOVED, parentIng.name(), parentIng.quantity(), parentIng.unit(), 0, ""));
            }
        }

        var stepDiffs   = new ArrayList<StepDiff>();
        var parentSize  = parent.steps().size();
        var variantSize = variant.steps().size();
        var minSize     = Math.min(parentSize, variantSize);
        for (var i = 0; i < minSize; i++) {
            var ps = parent.steps().get(i);
            var vs = variant.steps().get(i);
            stepDiffs.add(ps.instruction().equals(vs.instruction())
                ? new StepDiff(DiffType.UNCHANGED, vs.stepNumber(), vs.instruction(), "")
                : new StepDiff(DiffType.MODIFIED,  vs.stepNumber(), vs.instruction(), ps.instruction()));
        }
        for (var i = minSize; i < variantSize; i++) {
            var s = variant.steps().get(i);
            stepDiffs.add(new StepDiff(DiffType.ADDED, s.stepNumber(), s.instruction(), ""));
        }
        for (var i = minSize; i < parentSize; i++) {
            var s = parent.steps().get(i);
            stepDiffs.add(new StepDiff(DiffType.REMOVED, s.stepNumber(), s.instruction(), ""));
        }

        var parentAllergenKeys  = parent.allergens().stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        var variantAllergenKeys = variant.allergens().stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        var allergenDiffs = new ArrayList<AllergenDiff>();
        for (var a : variant.allergens()) {
            allergenDiffs.add(new AllergenDiff(
                parentAllergenKeys.contains(a.name().toLowerCase(Locale.ROOT)) ? DiffType.UNCHANGED : DiffType.ADDED,
                a.name(), a.severity()));
        }
        for (var a : parent.allergens()) {
            if (!variantAllergenKeys.contains(a.name().toLowerCase(Locale.ROOT))) {
                allergenDiffs.add(new AllergenDiff(DiffType.REMOVED, a.name(), a.severity()));
            }
        }

        Map<String, Boolean> parentFlags  = parent.dietaryFlags()  != null ? parent.dietaryFlags()  : Map.of();
        Map<String, Boolean> variantFlags = variant.dietaryFlags() != null ? variant.dietaryFlags() : Map.of();
        var allKeys = new HashSet<String>();
        allKeys.addAll(parentFlags.keySet());
        allKeys.addAll(variantFlags.keySet());
        var flagDiffs = new ArrayList<DietaryFlagDiff>();
        for (var key : allKeys) {
            var inParent  = parentFlags.getOrDefault(key, false);
            var inVariant = variantFlags.getOrDefault(key, false);
            if      (inParent && inVariant)  flagDiffs.add(new DietaryFlagDiff(DiffType.UNCHANGED, key));
            else if (!inParent && inVariant) flagDiffs.add(new DietaryFlagDiff(DiffType.ADDED,     key));
            else if (inParent) flagDiffs.add(new DietaryFlagDiff(DiffType.REMOVED,   key));
        }

        return new RecipeDiff(titleChanged, parent.title(), summaryChanged, parent.summary(),
            timeDelta, imageChanged, List.copyOf(ingDiffs), List.copyOf(stepDiffs), List.copyOf(allergenDiffs), List.copyOf(flagDiffs));
    }
}
