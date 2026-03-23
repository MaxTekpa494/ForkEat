package fr.uge.forkeat.service.strategy;

import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.recipe.RecipeDiff.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Strategie de diff complete : compare tous les champs entre parent et variante,
 * incluant les elements UNCHANGED.
 */
@Component
public class FullRecipeDiffStrategy implements RecipeDiffStrategy {

    @Override
    public RecipeDiff compute(Recipe parent, Recipe variant) {
        Objects.requireNonNull(parent);
        Objects.requireNonNull(variant);

        var titleChanged   = !Objects.equals(parent.title(), variant.title());
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

    private List<IngredientDiff> computeIngredientDiffs(Recipe parent, Recipe variant) {
        var parentIngByName  = new LinkedHashMap<String, RecipeIngredient>();
        var variantIngByName = new LinkedHashMap<String, RecipeIngredient>();
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

    private List<StepDiff> computeStepDiffs(Recipe parent, Recipe variant) {
        var diffs       = new ArrayList<StepDiff>();
        var parentSteps  = parent.stepByStepInstructions();
        var variantSteps = variant.stepByStepInstructions();
        var parentSize  = parentSteps.size();
        var variantSize = variantSteps.size();
        var minSize     = Math.min(parentSize, variantSize);

        for (var i = 0; i < minSize; i++) {
            var ps = parentSteps.get(i);
            var vs = variantSteps.get(i);
            diffs.add(ps.instruction().equals(vs.instruction())
                ? new StepDiff(DiffType.UNCHANGED, vs.stepNumber(), vs.instruction(), "")
                : new StepDiff(DiffType.MODIFIED,  vs.stepNumber(), vs.instruction(), ps.instruction()));
        }
        for (var i = minSize; i < variantSize; i++) {
            var s = variantSteps.get(i);
            diffs.add(new StepDiff(DiffType.ADDED, s.stepNumber(), s.instruction(), ""));
        }
        for (var i = minSize; i < parentSize; i++) {
            var s = parentSteps.get(i);
            diffs.add(new StepDiff(DiffType.REMOVED, s.stepNumber(), s.instruction(), ""));
        }
        return diffs;
    }

    private List<AllergenDiff> computeAllergenDiffs(Recipe parent, Recipe variant) {
        var parentKeys  = parent.allergens().stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        var variantKeys = variant.allergens().stream().map(a -> a.name().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());

        var diffs = new ArrayList<AllergenDiff>();
        for (var a : variant.allergens()) {
            diffs.add(new AllergenDiff(
                parentKeys.contains(a.name().toLowerCase(Locale.ROOT)) ? DiffType.UNCHANGED : DiffType.ADDED,
                a.name(), a.severity().name()));
        }
        for (var a : parent.allergens()) {
            if (!variantKeys.contains(a.name().toLowerCase(Locale.ROOT))) {
                diffs.add(new AllergenDiff(DiffType.REMOVED, a.name(), a.severity().name()));
            }
        }
        return diffs;
    }

    private List<DietaryFlagDiff> computeDietaryFlagDiffs(Recipe parent, Recipe variant) {
        var parentSet  = parent.dietaries()  != null ? new HashSet<>(parent.dietaries())  : new HashSet<String>();
        var variantSet = variant.dietaries() != null ? new HashSet<>(variant.dietaries()) : new HashSet<String>();
        var all = new HashSet<String>();
        all.addAll(parentSet);
        all.addAll(variantSet);

        var diffs = new ArrayList<DietaryFlagDiff>();
        for (var name : all) {
            var inParent  = parentSet.contains(name);
            var inVariant = variantSet.contains(name);
            if      (inParent && inVariant)  diffs.add(new DietaryFlagDiff(DiffType.UNCHANGED, name));
            else if (!inParent && inVariant) diffs.add(new DietaryFlagDiff(DiffType.ADDED,     name));
            else                             diffs.add(new DietaryFlagDiff(DiffType.REMOVED,   name));
        }
        return diffs;
    }
}
