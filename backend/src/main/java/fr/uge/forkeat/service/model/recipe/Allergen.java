package fr.uge.forkeat.service.model.recipe;

import java.util.UUID;

public record Allergen(
        UUID id,
        String name,
        AllergenSeverity severity
) {
  // LES VERIFS
}