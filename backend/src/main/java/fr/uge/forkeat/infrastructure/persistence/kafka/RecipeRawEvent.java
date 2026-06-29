package fr.uge.forkeat.infrastructure.persistence.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.uge.forkeat.infrastructure.persistence.mapper.RecipeEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeStep;
import fr.uge.forkeat.service.model.recipe.Allergen;
import fr.uge.forkeat.service.model.recipe.AllergenSeverity;
import fr.uge.forkeat.service.model.recipe.RecipeIngredient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecipeRawEvent(
        String title,
        String summary,
        @JsonProperty("ready_in_minutes")
        int preparationMinutes,
        @JsonProperty("image_url")
        String imageUrl,
        @JsonProperty("step_by_step_instructions")
        List<RecipeStep> stepByStepInstructions,
        @JsonProperty("ingredients_detailed")
        List<IngredientDetailDto> ingredientDetailed,
        List<String> allergens,

        // ------ Elements for dietary ------
        boolean vegetarian,
        boolean vegan,
        @JsonProperty("gluten_free")
        boolean glutenFree,
        @JsonProperty("diary_free")
        boolean diaryFree,
        @JsonProperty("very_healthy")
        boolean veryHealthy,
        @JsonProperty("low_fodmap")
        boolean lowFodMap
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record IngredientDetailDto(
          String name,
          @JsonProperty("amount")
          double quantity,
          String unit
  ){
    public IngredientDetailDto{
      Objects.requireNonNull(name, "Name in IngredientDetailDto cannot be null");
      Objects.requireNonNull(unit,"Unit in IngredientDetailDto cannot be null");
    }


  }

  public List<String> convertToDietaries(){
    var dietariesBoolean = Map.of( // PAS TRÈS BON ÇA... PAS HARDCODER...
            "vegetarian", vegetarian,
            "vegan", vegan,
            "glutenFree", glutenFree,
            "diaryFree", diaryFree,
            "veryHealthy", veryHealthy,
            "lowFodMap", lowFodMap
    );
    return dietariesBoolean.keySet()
            .stream()
            .filter(k -> dietariesBoolean.getOrDefault(k, false))
            .toList();
  }

  public List<RecipeIngredient> convertToRecipeIngredient(){
    return ingredientDetailed.stream()
            .map(r -> new RecipeIngredient(r.name, r.quantity, r.unit))
            .toList();
  }

  public List<Allergen> convertToAllergens(){
    return allergens.stream()
            .map(al -> new Allergen(UUID.randomUUID(), al, AllergenSeverity.MEDIUM))
            .toList();
  }

  public List<fr.uge.forkeat.service.model.recipe.RecipeStep> convertToRecipeStepModel(){
    return RecipeEntityMapper.toRecipeSteps(stepByStepInstructions);
  }
}
