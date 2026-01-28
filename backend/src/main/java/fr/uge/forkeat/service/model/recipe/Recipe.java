package fr.uge.forkeat.service.model.recipe;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record Recipe(
        UUID id,
        String title,
        String summary,
        UUID parentID,
        UUID authorID,
        int preparationMinutes,
        String imageUrl,
        RecipeStatus status,
        List<Map<String, Object>> stepByStepInstructions,
        List<UUID> allergens
) {

  public static Builder with(){
    return new Builder();
  }

  public static class Builder{
   private UUID id;
   private String title;
   private String summary;

  }


}
