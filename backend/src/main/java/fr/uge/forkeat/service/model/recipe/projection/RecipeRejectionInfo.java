package fr.uge.forkeat.service.model.recipe.projection;

import java.time.Instant;

public record RecipeRejectionInfo(String justification, Instant rejectedAt) {}