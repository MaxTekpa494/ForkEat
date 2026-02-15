package fr.uge.forkeat.service.model.recipe;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RecipeWithMetaData {
    private final Recipe recipe;
    private final RecipeMetaData recipeMetaData;

    public RecipeWithMetaData(Recipe recipe, RecipeMetaData recipeMetaData) {
        this.recipe = recipe;
        this.recipeMetaData = recipeMetaData;
    }



    public UUID id(){
        return recipe.id();
    }
    public String title(){
        return recipe.title();
    }
    public String summary(){
        return recipe.summary();
    }
    public UUID parentId(){
        return recipe.parentId();
    }
    public String usernameAuthor(){
        return recipe.usernameAuthor();
    }
    public int preparationMinutes(){
        return recipe.preparationMinutes();
    }
    public String imageUrl(){
        return recipe.imageUrl();
    }
    public RecipeStatus status(){
        return recipe.status();
    }
    public List<RecipeStep> stepByStepInstructions(){
        return recipe.stepByStepInstructions();
    }
    public List<Allergen> allergens(){
        return recipe.allergens();
    }
    public Map<String, Boolean> dietaryFlags(){
        return recipe.dietaryFlags();
    }
    public Instant createdAt(){
        return recipe.createdAt();
    }
    public Instant updatedAt(){
        return recipe.updatedAt();
    }
    public List<RecipeIngredient> ingredients(){
        return recipe.ingredients();
    }

    public boolean isVariant() { return recipe.isVariant(); }

    public boolean isPublished() {
        return recipe.isPublished();
    }

    public long nbLike(){
        return this.recipeMetaData.nbLike();
    }
}
