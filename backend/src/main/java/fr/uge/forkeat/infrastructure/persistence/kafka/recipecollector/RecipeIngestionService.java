package fr.uge.forkeat.infrastructure.persistence.kafka.recipecollector;

import fr.uge.forkeat.infrastructure.persistence.adapter.RecipePersistenceAdapter;
import fr.uge.forkeat.infrastructure.persistence.mapper.RecipeEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.AllergenEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.AllergenRepository;
import fr.uge.forkeat.service.model.recipe.Allergen;
import fr.uge.forkeat.service.model.recipe.AllergenSeverity;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
class RecipeIngestionService {

  private final RecipePersistenceAdapter recipePersistenceAdapter; // Une sorte de proxy composition
  private final AllergenRepository allergenRepository;
  private final String userSystem;

  RecipeIngestionService(RecipePersistenceAdapter recipePersistenceAdapter, AllergenRepository allergenRepository, @Value("${app.system.earnings.username}") String userSystem){
    this.recipePersistenceAdapter = recipePersistenceAdapter;
    this.allergenRepository = allergenRepository;
    this.userSystem = userSystem;
  }

  private List<Allergen> resolveAllergens(RecipeRawEvent event){
    if (event.allergens() == null) {
      return List.of();
    }
    return event.allergens().stream()
            .map(name -> allergenRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> allergenRepository.save(new AllergenEntity(name, AllergenSeverity.MEDIUM))))
            .map(RecipeEntityMapper::toDomain)
            .toList();
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
            resolveAllergens(event),
            event.convertToDietaries(),
            Instant.now(),
            Instant.now()
    );
  }

  @Transactional
  Recipe ingest(RecipeRawEvent event){
    Objects.requireNonNull(event);
    return recipePersistenceAdapter.save(convertRecipeRawEventToRecipeModel(event));
  }

}
