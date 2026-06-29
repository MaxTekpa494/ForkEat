package fr.uge.forkeat.infrastructure.persistence.kafka;

import fr.uge.forkeat.infrastructure.persistence.adapter.RecipePersistenceAdapter;
import fr.uge.forkeat.service.model.recipe.Recipe;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
class RecipeIngestionService {

  private final RecipePersistenceAdapter recipePersistenceAdapter; // Une sorte de proxy composition

  RecipeIngestionService(RecipePersistenceAdapter recipePersistenceAdapter){
    this.recipePersistenceAdapter = recipePersistenceAdapter;
  }

  private Recipe convertRecipeRawEventToRecipe(RecipeRawEvent event){
    var recipe = new Recipe(UUID.randomUUID(),
            event.title(),
            event.summary(),
            event.
    );

    /*
    String title,
    String summary,
    UUID parentId,
    String usernameAuthor,
    int preparationMinutes,
    String imageUrl,
    RecipeStatus status,
    List<RecipeStep> stepByStepInstructions,
    List<RecipeIngredient> ingredients,
    List<Allergen> allergens,
    List<String> dietaries,
    Instant createdAt,
    Instant updatedAt
     */
  }

  void ingest(RecipeRawEvent event){
    Objects.requireNonNull(event);
    recipePersistenceAdapter.save()
  }

}
