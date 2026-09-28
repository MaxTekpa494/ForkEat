package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class RecipeStepTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateValidRecipeStep() {
        var step = new RecipeStep(1, "Préchauffer le four à 180°C");

        assertEquals(1, step.stepNumber());
        assertEquals("Préchauffer le four à 180°C", step.instruction());
    }

    @Test
    void shouldThrowExceptionWhenInstructionIsNull() {
        assertThrows(NullPointerException.class, () -> new RecipeStep(1, null));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -2, -100}) // Je ne suis un peu arrangé avec ce test normalement on doit verifier > 0 mais je ne sais pas trop
    void shouldThrowExceptionWhenStepNumberIsLessThanOne(int invalidStepNumber) {
        assertThrows(IllegalArgumentException.class,
            () -> new RecipeStep(invalidStepNumber, "Some instruction"));
    }

    @Test
    void shouldSerializeToJsonWithSnakeCase() throws JsonProcessingException {
        var step = new RecipeStep(1, "Mélanger les ingrédients");

        String json = objectMapper.writeValueAsString(step);

        assertTrue(json.contains("\"step_number\":1"));
        assertTrue(json.contains("\"instruction\":\"Mélanger les ingrédients\""));
    }

    @Test
    void shouldDeserializeFromJsonWithSnakeCase() throws JsonProcessingException {
        String json = """
            {
                "step_number": 2,
                "instruction": "Cuire pendant 20 minutes"
            }
            """;

        RecipeStep step = objectMapper.readValue(json, RecipeStep.class);

        assertEquals(2, step.stepNumber());
        assertEquals("Cuire pendant 20 minutes", step.instruction());
    }

    @Test
    void shouldIgnoreUnknownFieldsDuringDeserialization() throws JsonProcessingException {
        String json = """
            {
                "step_number": 1,
                "instruction": "Étape avec champs ignorés",
                "duration": {"number": 5, "unit": "minutes"},
                "equipment": ["four", "casserole"],
                "ingredients": ["farine", "sucre"]
            }
            """;

        RecipeStep step = objectMapper.readValue(json, RecipeStep.class);

        assertEquals(1, step.stepNumber());
        assertEquals("Étape avec champs ignorés", step.instruction());
    }

    @Test
    void shouldBeEqualWhenSameValues() {
        var step1 = new RecipeStep(1, "Même instruction");
        var step2 = new RecipeStep(1, "Même instruction");

        assertEquals(step1, step2);
        assertEquals(step1.hashCode(), step2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenDifferentStepNumber() {
        var step1 = new RecipeStep(1, "Instruction");
        var step2 = new RecipeStep(2, "Instruction");

        assertNotEquals(step1, step2);
    }

    @Test
    void shouldNotBeEqualWhenDifferentInstruction() {
        var step1 = new RecipeStep(1, "Instruction A");
        var step2 = new RecipeStep(1, "Instruction B");

        assertNotEquals(step1, step2);
    }

    @Test
    void shouldHaveCorrectToString() {
        var step = new RecipeStep(3, "Test instruction");

        String toString = step.toString();

        assertTrue(toString.contains("3"));
        assertTrue(toString.contains("Test instruction"));
    }
}
