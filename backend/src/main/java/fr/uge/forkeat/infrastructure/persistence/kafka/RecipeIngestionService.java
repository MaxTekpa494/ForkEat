package fr.uge.forkeat.infrastructure.persistence.kafka;

import fr.uge.forkeat.infrastructure.persistence.adapter.RecipePersistenceAdapter;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
class RecipeIngestionService {

  private final RecipePersistenceAdapter recipePersistenceAdapter; // Une sorte de proxy composition
  private final String userSystem;

  RecipeIngestionService(RecipePersistenceAdapter recipePersistenceAdapter, @Value("${app.system.earnings.username}") String userSystem){
    this.recipePersistenceAdapter = recipePersistenceAdapter;
    this.userSystem = userSystem;
  }

  private Recipe convertRecipeRawEventToRecipeModel(RecipeRawEvent event){
    return new Recipe(
            UUID.randomUUID(),
            event.title(),
            event.summary(),
            null, // pas de parent
            userSystem,
            event.preparationMinutes(),
            event.imageUrl(),
            RecipeStatus.DRAFT,
            event.convertToRecipeStepModel(),
            event.convertToRecipeIngredient(),
            event.convertToAllergens(),
            event.convertToDietaries(),
            Instant.now(),
            Instant.now()
    );
  }

  Recipe ingest(RecipeRawEvent event){
    Objects.requireNonNull(event);
    return recipePersistenceAdapter.save(convertRecipeRawEventToRecipeModel(event));
  }

}
