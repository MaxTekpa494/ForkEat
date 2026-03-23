package fr.uge.forkeat.service.strategy;

import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.recipe.RecipeDiff.DiffType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FullRecipeDiffStrategyTest {

    private final FullRecipeDiffStrategy strategy = new FullRecipeDiffStrategy();

    private static Recipe recipe(String title, String summary, int minutes, String imageUrl,
                                 List<RecipeIngredient> ingredients,
                                 List<RecipeStep> steps,
                                 List<Allergen> allergens,
                                 List<String> flags) {
        return new Recipe(UUID.randomUUID(), title, summary, null, "user", minutes,
                imageUrl, RecipeStatus.PUBLISHED, steps, ingredients, allergens, flags, null, null);
    }

    private static RecipeIngredient ing(String name, double qty, String unit) {
        return new RecipeIngredient(name, qty, unit);
    }

    private static RecipeStep step(int num, String instruction) {
        return new RecipeStep(num, instruction);
    }

    private static Allergen allergen(String name) {
        return new Allergen(UUID.randomUUID(), name, AllergenSeverity.MEDIUM);
    }


    @Test
    void shouldThrowOnNullParent() {
        var variant = recipe("V", "s", 30, null, List.of(), List.of(), List.of(), List.of());
        assertThrows(NullPointerException.class, () -> strategy.compute(null, variant));
    }

    @Test
    void shouldThrowOnNullVariant() {
        var parent = recipe("P", "s", 30, null, List.of(), List.of(), List.of(), List.of());
        assertThrows(NullPointerException.class, () -> strategy.compute(parent, null));
    }


    @Test
    void shouldDetectNoChangesWhenIdentical() {
        var parent  = recipe("Tarte", "desc", 30, "img.jpg", List.of(), List.of(), List.of(), List.of());
        var variant = recipe("Tarte", "desc", 30, "img.jpg", List.of(), List.of(), List.of(), List.of());

        var diff = strategy.compute(parent, variant);

        assertFalse(diff.titleChanged());
        assertFalse(diff.summaryChanged());
        assertEquals(0, diff.timeDelta());
        assertFalse(diff.imageChanged());
    }

    @Test
    void shouldDetectTitleChanged() {
        var parent  = recipe("Tarte aux pommes", "desc", 30, null, List.of(), List.of(), List.of(), List.of());
        var variant = recipe("Tarte allégée",    "desc", 30, null, List.of(), List.of(), List.of(), List.of());

        var diff = strategy.compute(parent, variant);

        assertTrue(diff.titleChanged());
        assertEquals("Tarte aux pommes", diff.originalTitle());
    }

    @Test
    void shouldDetectSummaryChanged() {
        var parent  = recipe("T", "Description originale", 30, null, List.of(), List.of(), List.of(), List.of());
        var variant = recipe("T", "Nouvelle description",  30, null, List.of(), List.of(), List.of(), List.of());

        var diff = strategy.compute(parent, variant);

        assertTrue(diff.summaryChanged());
        assertEquals("Description originale", diff.originalSummary());
    }

    @Test
    void shouldComputePositiveTimeDelta() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), List.of());
        var variant = recipe("T", "s", 45, null, List.of(), List.of(), List.of(), List.of());

        assertEquals(15, strategy.compute(parent, variant).timeDelta());
    }

    @Test
    void shouldComputeNegativeTimeDelta() {
        var parent  = recipe("T", "s", 60, null, List.of(), List.of(), List.of(), List.of());
        var variant = recipe("T", "s", 40, null, List.of(), List.of(), List.of(), List.of());

        assertEquals(-20, strategy.compute(parent, variant).timeDelta());
    }

    @Test
    void shouldDetectImageChanged() {
        var parent  = recipe("T", "s", 30, "old.jpg",  List.of(), List.of(), List.of(), List.of());
        var variant = recipe("T", "s", 30, "new.jpg",  List.of(), List.of(), List.of(), List.of());

        assertTrue(strategy.compute(parent, variant).imageChanged());
    }

    @Test
    void shouldMarkIngredientAsUnchanged() {
        var ing     = ing("Farine", 200, "g");
        var parent  = recipe("T", "s", 30, null, List.of(ing), List.of(), List.of(), List.of());
        var variant = recipe("T", "s", 30, null, List.of(ing), List.of(), List.of(), List.of());

        var diff = strategy.compute(parent, variant);

        assertEquals(1, diff.ingredients().size());
        assertEquals(DiffType.UNCHANGED, diff.ingredients().getFirst().type());
    }

    @Test
    void shouldMarkIngredientAsAdded() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), List.of());
        var variant = recipe("T", "s", 30, null, List.of(ing("Cannelle", 1, "c.s")), List.of(), List.of(), List.of());

        var diff = strategy.compute(parent, variant);

        assertEquals(1, diff.ingredients().size());
        var ingDiff = diff.ingredients().getFirst();
        assertEquals(DiffType.ADDED, ingDiff.type());
        assertEquals("Cannelle", ingDiff.name());
    }

    @Test
    void shouldMarkIngredientAsRemoved() {
        var parent  = recipe("T", "s", 30, null, List.of(ing("Lait", 100, "ml")), List.of(), List.of(), List.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), List.of());

        var diff = strategy.compute(parent, variant);

        assertEquals(1, diff.ingredients().size());
        var ingDiff = diff.ingredients().getFirst();
        assertEquals(DiffType.REMOVED, ingDiff.type());
        assertEquals("Lait", ingDiff.name());
    }

    @Test
    void shouldMarkIngredientAsModifiedWhenQuantityChanges() {
        var parent  = recipe("T", "s", 30, null, List.of(ing("Beurre", 200, "g")), List.of(), List.of(), List.of());
        var variant = recipe("T", "s", 30, null, List.of(ing("Beurre", 150, "g")), List.of(), List.of(), List.of());

        var diff = strategy.compute(parent, variant);

        assertEquals(1, diff.ingredients().size());
        var ingDiff = diff.ingredients().getFirst();
        assertEquals(DiffType.MODIFIED, ingDiff.type());
        assertEquals(150,   ingDiff.quantity(), 0.001);
        assertEquals(200,   ingDiff.originalQuantity(), 0.001);
    }

    @Test
    void shouldMarkIngredientAsModifiedWhenUnitChanges() {
        var parent  = recipe("T", "s", 30, null, List.of(ing("Eau", 1, "L")),   List.of(), List.of(), List.of());
        var variant = recipe("T", "s", 30, null, List.of(ing("Eau", 1, "ml")), List.of(), List.of(), List.of());

        var ingDiff = strategy.compute(parent, variant).ingredients().getFirst();

        assertEquals(DiffType.MODIFIED, ingDiff.type());
        assertEquals("ml", ingDiff.unit());
        assertEquals("L",  ingDiff.originalUnit());
    }

    @Test
    void shouldMatchIngredientsCaseInsensitively() {
        var parent  = recipe("T", "s", 30, null, List.of(ing("FARINE", 200, "g")), List.of(), List.of(), List.of());
        var variant = recipe("T", "s", 30, null, List.of(ing("farine", 200, "g")), List.of(), List.of(), List.of());

        var ingDiff = strategy.compute(parent, variant).ingredients().getFirst();

        assertEquals(DiffType.UNCHANGED, ingDiff.type());
    }


    @Test
    void shouldMarkStepAsUnchanged() {
        var s       = step(1, "Mélanger la farine et les œufs");
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(s), List.of(), List.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(s), List.of(), List.of());

        var stepDiff = strategy.compute(parent, variant).steps().getFirst();

        assertEquals(DiffType.UNCHANGED, stepDiff.type());
    }

    @Test
    void shouldMarkStepAsModified() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(step(1, "Ancienne instruction")), List.of(), List.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(step(1, "Nouvelle instruction")), List.of(), List.of());

        var stepDiff = strategy.compute(parent, variant).steps().getFirst();

        assertEquals(DiffType.MODIFIED, stepDiff.type());
        assertEquals("Nouvelle instruction", stepDiff.instruction());
        assertEquals("Ancienne instruction", stepDiff.originalInstruction());
    }

    @Test
    void shouldMarkExtraVariantStepAsAdded() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(step(1, "Étape 1")), List.of(), List.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(step(1, "Étape 1"), step(2, "Nouvelle étape")), List.of(), List.of());

        var steps = strategy.compute(parent, variant).steps();

        assertEquals(2, steps.size());
        assertEquals(DiffType.UNCHANGED, steps.get(0).type());
        assertEquals(DiffType.ADDED,     steps.get(1).type());
    }

    @Test
    void shouldMarkMissingVariantStepAsRemoved() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(step(1, "Étape 1"), step(2, "Étape 2")), List.of(), List.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(step(1, "Étape 1")), List.of(), List.of());

        var steps = strategy.compute(parent, variant).steps();

        assertEquals(2, steps.size());
        assertEquals(DiffType.UNCHANGED, steps.get(0).type());
        assertEquals(DiffType.REMOVED,   steps.get(1).type());
        assertEquals("Étape 2", steps.get(1).instruction());
    }


    @Test
    void shouldMarkAllergenAsUnchanged() {
        var a       = allergen("Gluten");
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(a), List.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(), List.of(a), List.of());

        var allergenDiff = strategy.compute(parent, variant).allergens().getFirst();

        assertEquals(DiffType.UNCHANGED, allergenDiff.type());
        assertEquals("Gluten", allergenDiff.name());
    }

    @Test
    void shouldMarkAllergenAsAdded() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), List.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(), List.of(allergen("Soja")), List.of());

        var allergenDiff = strategy.compute(parent, variant).allergens().getFirst();

        assertEquals(DiffType.ADDED, allergenDiff.type());
        assertEquals("Soja", allergenDiff.name());
    }

    @Test
    void shouldMarkAllergenAsRemoved() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(allergen("Lait")), List.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), List.of());

        var allergenDiff = strategy.compute(parent, variant).allergens().getFirst();

        assertEquals(DiffType.REMOVED, allergenDiff.type());
        assertEquals("Lait", allergenDiff.name());
    }


    @Test
    void shouldMarkDietaryFlagAsUnchanged() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), List.of("vegan"));
        var variant = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), List.of("vegan"));

        var flagDiff = strategy.compute(parent, variant).dietaryFlags().stream()
                .filter(f -> f.flagName().equals("vegan")).findFirst().orElseThrow();

        assertEquals(DiffType.UNCHANGED, flagDiff.type());
    }

    @Test
    void shouldMarkDietaryFlagAsAdded() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), List.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), List.of("gluten-free"));

        var flagDiff = strategy.compute(parent, variant).dietaryFlags().stream()
                .filter(f -> f.flagName().equals("gluten-free")).findFirst().orElseThrow();

        assertEquals(DiffType.ADDED, flagDiff.type());
    }

    @Test
    void shouldMarkDietaryFlagAsRemoved() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), List.of("halal"));
        var variant = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), List.of());

        var flagDiff = strategy.compute(parent, variant).dietaryFlags().stream()
                .filter(f -> f.flagName().equals("halal")).findFirst().orElseThrow();

        assertEquals(DiffType.REMOVED, flagDiff.type());
    }

    @Test
    void shouldReturnNoDietaryDiffsWhenBothHaveNone() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), List.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), List.of());

        var flags = strategy.compute(parent, variant).dietaryFlags();

        assertTrue(flags.isEmpty());
    }
}
