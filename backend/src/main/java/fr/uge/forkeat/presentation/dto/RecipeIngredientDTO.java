package fr.uge.forkeat.presentation.dto;

public record RecipeIngredientDTO(
        String name,
        double quantity,
        String unit
) {
  // Les verifs ...
}
