package fr.uge.forkeat.presentation.dto.recipe;

import java.util.UUID;

public record AllergenDTO(UUID id, String name, String severity) {
  // LES VERIFS ...
}
