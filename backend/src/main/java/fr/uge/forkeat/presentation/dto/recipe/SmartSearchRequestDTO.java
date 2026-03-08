package fr.uge.forkeat.presentation.dto.recipe;

import java.util.Objects;

public record SmartSearchRequestDTO(String query) {
  public SmartSearchRequestDTO {
    if (query == null || query.isBlank()) {
      throw new IllegalArgumentException("La requête de recherche ne peut pas être vide.");
    }
  }
}
