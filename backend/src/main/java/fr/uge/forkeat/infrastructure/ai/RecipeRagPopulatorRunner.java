package fr.uge.forkeat.infrastructure.ai;

import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRagRepository;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public final class RecipeRagPopulatorRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(RecipeRagPopulatorRunner.class);

  private final RecipeRagIndexingService recipeRagIndexingService;
  private final RecipeRagRepository recipeRagRepository;

  public RecipeRagPopulatorRunner(RecipeRagIndexingService recipeRagIndexingService, RecipeRagRepository recipeRagRepository) {
    this.recipeRagIndexingService = recipeRagIndexingService;
    this.recipeRagRepository = recipeRagRepository;
  }

  @Override
  public void run(@NonNull ApplicationArguments args) throws Exception {
    Thread.ofVirtual().start(this::populateRag); // Du coup au demarrage, on ne bloque pas le Thread principal
  }

  private void populateRag(){
    var missingIds = recipeRagRepository.findRecipeIdsWithoutRagEntry();
    if(missingIds.isEmpty()){
      log.info("[RAG] Base vectorielle à jour, aucune recette à indexer");
      return;
    }
    log.info("[RAG] Indexation de {} recette(s)...", missingIds.size());
    var succes = 0;
    var failed = 0;
    for(var recipeId : missingIds){
      try{
        recipeRagIndexingService.indexRecipe(recipeId);
        succes++;
        log.info("[RAG] {}/{} — recette {} indexée.",
                succes + failed, missingIds.size(), recipeId);
      }catch (Exception e){
        // Bon noprmalement c'est juste un IllegalArgumentException ici mais je catch tout quand même,
        // On ne sait jamais, et en plus vue que c'est lancé dans un Thread
        failed++;
        log.error("[RAG] Échec indexation recette {} : {}", recipeId, e.getMessage());
      }
    }
    log.info("[RAG] Terminé — {} succès, {} échec(s).", succes, failed);
  }
}
