package fr.uge.forkeat.presentation.dto.recipe;

import java.util.Objects;

public record RecipeIngredientDTO(String name, double quantity, String unit) {
  public RecipeIngredientDTO{
    Objects.requireNonNull(name);
    Objects.requireNonNull(unit);
  }
}
