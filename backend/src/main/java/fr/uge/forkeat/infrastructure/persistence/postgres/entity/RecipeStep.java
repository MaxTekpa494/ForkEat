package fr.uge.forkeat.infrastructure.persistence.postgres.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecipeStep(
        @JsonProperty("step_number") int stepNumber,
        @JsonProperty("instruction") String instruction
) {
    public RecipeStep {
        Objects.requireNonNull(instruction, "Instruction cannot be null");
        if(stepNumber < 0){
            throw new IllegalArgumentException("stepNumbe must be > 0");
        }
    }
}