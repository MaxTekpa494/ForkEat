package fr.uge.forkeat.infrastructure.ai;

import fr.uge.forkeat.service.event.RecipePublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RecipeRagPublishedListener {
  private static final Logger log =  LoggerFactory.getLogger(RecipeRagPublishedListener.class);
  private final RecipeRagIndexingService recipeRagIndexingService;
  public RecipeRagPublishedListener(RecipeRagIndexingService recipeRagIndexingService) {
    this.recipeRagIndexingService = recipeRagIndexingService;
  }

  /**
   * S'exécute APRÈS le commit de la transaction qui a mis la recette en PUBLISHED.
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRecipePublished(RecipePublishedEvent event) {
    Thread.ofVirtual()
            .name("recipe-rag-"+event.recipeId())
            .start(() ->{
              try {
                log.info("[RAG] Indexation de la recette {} (publication détectée)...", event.recipeId());
                recipeRagIndexingService.indexRecipe(event.recipeId());
                log.info("[RAG] Recette {} indexée avec succès.", event.recipeId());
              } catch (Exception e) {
                log.error("[RAG] Échec indexation recette {} : {}", event.recipeId(), e.getMessage(), e);
              }
            });
  }
}
