package fr.uge.forkeat.service.model.recipe;

import java.util.Objects;

public record RecipeStep(int stepNumber, String instruction) {
    public RecipeStep {
        Objects.requireNonNull(instruction, "Instruction cannot be null");
        if(stepNumber < 0){
            throw new IllegalArgumentException("stepNumbe must be > 0");
        }
    }
}