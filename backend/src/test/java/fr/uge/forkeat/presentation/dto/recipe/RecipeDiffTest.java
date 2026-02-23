package fr.uge.forkeat.presentation.dto.recipe;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecipeDiffTest {

    private static RecipeDTO recipe(String title, String summary, int minutes, String imageUrl,
                                    List<RecipeIngredientDTO> ingredients,
                                    List<RecipeStepDTO> steps,
                                    List<AllergenDTO> allergens,
                                    Map<String, Boolean> flags) {
        return new RecipeDTO(UUID.randomUUID(), title, summary, null, "user", minutes,
                imageUrl, "PUBLISHED", steps, ingredients, allergens, flags, null, null);
    }

    private static RecipeIngredientDTO ing(String name, double qty, String unit) {
        return new RecipeIngredientDTO(name, qty, unit);
    }

    private static RecipeStepDTO step(int num, String instruction) {
        return new RecipeStepDTO(num, instruction);
    }

    private static AllergenDTO allergen(String name) {
        return new AllergenDTO(UUID.randomUUID(), name, "MEDIUM");
    }


    @Test
    void shouldThrowOnNullParent() {
        var variant = recipe("V", "s", 30, null, List.of(), List.of(), List.of(), Map.of());
        assertThrows(NullPointerException.class, () -> RecipeDiff.compute(null, variant));
    }

    @Test
    void shouldThrowOnNullVariant() {
        var parent = recipe("P", "s", 30, null, List.of(), List.of(), List.of(), Map.of());
        assertThrows(NullPointerException.class, () -> RecipeDiff.compute(parent, null));
    }


    @Test
    void shouldDetectNoChangesWhenIdentical() {
        var parent  = recipe("Tarte", "desc", 30, "img.jpg", List.of(), List.of(), List.of(), Map.of());
        var variant = recipe("Tarte", "desc", 30, "img.jpg", List.of(), List.of(), List.of(), Map.of());

        var diff = RecipeDiff.compute(parent, variant);

        assertFalse(diff.titleChanged());
        assertFalse(diff.summaryChanged());
        assertEquals(0, diff.timeDelta());
        assertFalse(diff.imageChanged());
    }

    @Test
    void shouldDetectTitleChanged() {
        var parent  = recipe("Tarte aux pommes", "desc", 30, null, List.of(), List.of(), List.of(), Map.of());
        var variant = recipe("Tarte allégée",    "desc", 30, null, List.of(), List.of(), List.of(), Map.of());

        var diff = RecipeDiff.compute(parent, variant);

        assertTrue(diff.titleChanged());
        assertEquals("Tarte aux pommes", diff.originalTitle());
    }

    @Test
    void shouldDetectSummaryChanged() {
        var parent  = recipe("T", "Description originale", 30, null, List.of(), List.of(), List.of(), Map.of());
        var variant = recipe("T", "Nouvelle description",  30, null, List.of(), List.of(), List.of(), Map.of());

        var diff = RecipeDiff.compute(parent, variant);

        assertTrue(diff.summaryChanged());
        assertEquals("Description originale", diff.originalSummary());
    }

    @Test
    void shouldComputePositiveTimeDelta() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), Map.of());
        var variant = recipe("T", "s", 45, null, List.of(), List.of(), List.of(), Map.of());

        assertEquals(15, RecipeDiff.compute(parent, variant).timeDelta());
    }

    @Test
    void shouldComputeNegativeTimeDelta() {
        var parent  = recipe("T", "s", 60, null, List.of(), List.of(), List.of(), Map.of());
        var variant = recipe("T", "s", 40, null, List.of(), List.of(), List.of(), Map.of());

        assertEquals(-20, RecipeDiff.compute(parent, variant).timeDelta());
    }

    @Test
    void shouldDetectImageChanged() {
        var parent  = recipe("T", "s", 30, "old.jpg",  List.of(), List.of(), List.of(), Map.of());
        var variant = recipe("T", "s", 30, "new.jpg",  List.of(), List.of(), List.of(), Map.of());

        assertTrue(RecipeDiff.compute(parent, variant).imageChanged());
    }

    @Test
    void shouldMarkIngredientAsUnchanged() {
        var ing     = ing("Farine", 200, "g");
        var parent  = recipe("T", "s", 30, null, List.of(ing), List.of(), List.of(), Map.of());
        var variant = recipe("T", "s", 30, null, List.of(ing), List.of(), List.of(), Map.of());

        var diff = RecipeDiff.compute(parent, variant);

        assertEquals(1, diff.ingredients().size());
        assertEquals(RecipeDiff.DiffType.UNCHANGED, diff.ingredients().getFirst().type());
    }

    @Test
    void shouldMarkIngredientAsAdded() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), Map.of());
        var variant = recipe("T", "s", 30, null, List.of(ing("Cannelle", 1, "c.s")), List.of(), List.of(), Map.of());

        var diff = RecipeDiff.compute(parent, variant);

        assertEquals(1, diff.ingredients().size());
        var ingDiff = diff.ingredients().getFirst();
        assertEquals(RecipeDiff.DiffType.ADDED, ingDiff.type());
        assertEquals("Cannelle", ingDiff.name());
    }

    @Test
    void shouldMarkIngredientAsRemoved() {
        var parent  = recipe("T", "s", 30, null, List.of(ing("Lait", 100, "ml")), List.of(), List.of(), Map.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), Map.of());

        var diff = RecipeDiff.compute(parent, variant);

        assertEquals(1, diff.ingredients().size());
        var ingDiff = diff.ingredients().getFirst();
        assertEquals(RecipeDiff.DiffType.REMOVED, ingDiff.type());
        assertEquals("Lait", ingDiff.name());
    }

    @Test
    void shouldMarkIngredientAsModifiedWhenQuantityChanges() {
        var parent  = recipe("T", "s", 30, null, List.of(ing("Beurre", 200, "g")), List.of(), List.of(), Map.of());
        var variant = recipe("T", "s", 30, null, List.of(ing("Beurre", 150, "g")), List.of(), List.of(), Map.of());

        var diff = RecipeDiff.compute(parent, variant);

        assertEquals(1, diff.ingredients().size());
        var ingDiff = diff.ingredients().getFirst();
        assertEquals(RecipeDiff.DiffType.MODIFIED, ingDiff.type());
        assertEquals(150,   ingDiff.quantity(), 0.001);
        assertEquals(200,   ingDiff.originalQuantity(), 0.001);
    }

    @Test
    void shouldMarkIngredientAsModifiedWhenUnitChanges() {
        var parent  = recipe("T", "s", 30, null, List.of(ing("Eau", 1, "L")),   List.of(), List.of(), Map.of());
        var variant = recipe("T", "s", 30, null, List.of(ing("Eau", 1, "ml")), List.of(), List.of(), Map.of());

        var ingDiff = RecipeDiff.compute(parent, variant).ingredients().getFirst();

        assertEquals(RecipeDiff.DiffType.MODIFIED, ingDiff.type());
        assertEquals("ml", ingDiff.unit());
        assertEquals("L",  ingDiff.originalUnit());
    }

    @Test
    void shouldMatchIngredientsCaseInsensitively() {
        var parent  = recipe("T", "s", 30, null, List.of(ing("FARINE", 200, "g")), List.of(), List.of(), Map.of());
        var variant = recipe("T", "s", 30, null, List.of(ing("farine", 200, "g")), List.of(), List.of(), Map.of());

        var ingDiff = RecipeDiff.compute(parent, variant).ingredients().getFirst();

        assertEquals(RecipeDiff.DiffType.UNCHANGED, ingDiff.type());
    }


    @Test
    void shouldMarkStepAsUnchanged() {
        var s       = step(1, "Mélanger la farine et les œufs");
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(s), List.of(), Map.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(s), List.of(), Map.of());

        var stepDiff = RecipeDiff.compute(parent, variant).steps().getFirst();

        assertEquals(RecipeDiff.DiffType.UNCHANGED, stepDiff.type());
    }

    @Test
    void shouldMarkStepAsModified() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(step(1, "Ancienne instruction")), List.of(), Map.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(step(1, "Nouvelle instruction")), List.of(), Map.of());

        var stepDiff = RecipeDiff.compute(parent, variant).steps().getFirst();

        assertEquals(RecipeDiff.DiffType.MODIFIED, stepDiff.type());
        assertEquals("Nouvelle instruction", stepDiff.instruction());
        assertEquals("Ancienne instruction", stepDiff.originalInstruction());
    }

    @Test
    void shouldMarkExtraVariantStepAsAdded() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(step(1, "Étape 1")), List.of(), Map.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(step(1, "Étape 1"), step(2, "Nouvelle étape")), List.of(), Map.of());

        var steps = RecipeDiff.compute(parent, variant).steps();

        assertEquals(2, steps.size());
        assertEquals(RecipeDiff.DiffType.UNCHANGED, steps.get(0).type());
        assertEquals(RecipeDiff.DiffType.ADDED,     steps.get(1).type());
    }

    @Test
    void shouldMarkMissingVariantStepAsRemoved() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(step(1, "Étape 1"), step(2, "Étape 2")), List.of(), Map.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(step(1, "Étape 1")), List.of(), Map.of());

        var steps = RecipeDiff.compute(parent, variant).steps();

        assertEquals(2, steps.size());
        assertEquals(RecipeDiff.DiffType.UNCHANGED, steps.get(0).type());
        assertEquals(RecipeDiff.DiffType.REMOVED,   steps.get(1).type());
        assertEquals("Étape 2", steps.get(1).instruction());
    }


    @Test
    void shouldMarkAllergenAsUnchanged() {
        var a       = allergen("Gluten");
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(a), Map.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(), List.of(a), Map.of());

        var allergenDiff = RecipeDiff.compute(parent, variant).allergens().getFirst();

        assertEquals(RecipeDiff.DiffType.UNCHANGED, allergenDiff.type());
        assertEquals("Gluten", allergenDiff.name());
    }

    @Test
    void shouldMarkAllergenAsAdded() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), Map.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(), List.of(allergen("Soja")), Map.of());

        var allergenDiff = RecipeDiff.compute(parent, variant).allergens().getFirst();

        assertEquals(RecipeDiff.DiffType.ADDED, allergenDiff.type());
        assertEquals("Soja", allergenDiff.name());
    }

    @Test
    void shouldMarkAllergenAsRemoved() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(allergen("Lait")), Map.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), Map.of());

        var allergenDiff = RecipeDiff.compute(parent, variant).allergens().getFirst();

        assertEquals(RecipeDiff.DiffType.REMOVED, allergenDiff.type());
        assertEquals("Lait", allergenDiff.name());
    }


    @Test
    void shouldMarkDietaryFlagAsUnchanged() {
        var flags   = Map.of("vegan", true);
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), flags);
        var variant = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), flags);

        var flagDiff = RecipeDiff.compute(parent, variant).dietaryFlags().stream()
                .filter(f -> f.flagName().equals("vegan")).findFirst().orElseThrow();

        assertEquals(RecipeDiff.DiffType.UNCHANGED, flagDiff.type());
    }

    @Test
    void shouldMarkDietaryFlagAsAdded() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), Map.of());
        var variant = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), Map.of("gluten-free", true));

        var flagDiff = RecipeDiff.compute(parent, variant).dietaryFlags().stream()
                .filter(f -> f.flagName().equals("gluten-free")).findFirst().orElseThrow();

        assertEquals(RecipeDiff.DiffType.ADDED, flagDiff.type());
    }

    @Test
    void shouldMarkDietaryFlagAsRemoved() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), Map.of("halal", true));
        var variant = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), Map.of("halal", false));

        var flagDiff = RecipeDiff.compute(parent, variant).dietaryFlags().stream()
                .filter(f -> f.flagName().equals("halal")).findFirst().orElseThrow();

        assertEquals(RecipeDiff.DiffType.REMOVED, flagDiff.type());
    }

    @Test
    void shouldIgnoreFalseFlagsInBothParentAndVariant() {
        var parent  = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), Map.of("vegan", false));
        var variant = recipe("T", "s", 30, null, List.of(), List.of(), List.of(), Map.of("vegan", false));

        var flags = RecipeDiff.compute(parent, variant).dietaryFlags();

        assertTrue(flags.isEmpty());
    }
}
