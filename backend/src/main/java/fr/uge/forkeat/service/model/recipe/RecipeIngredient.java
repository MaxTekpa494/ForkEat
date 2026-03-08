package fr.uge.forkeat.service.model.recipe;

import java.util.Objects;

public record RecipeIngredient(String name, double quantity, String unit) {

  public RecipeIngredient{
    Objects.requireNonNull(name);
    //Objects.requireNonNull(unit);
    if(quantity < 0){
      throw new IllegalArgumentException("RecipeIngredient : quantity < 0");
    }
  }
}