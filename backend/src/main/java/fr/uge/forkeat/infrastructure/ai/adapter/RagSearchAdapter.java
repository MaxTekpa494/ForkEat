package fr.uge.forkeat.infrastructure.ai.adapter;

import fr.uge.forkeat.infrastructure.ai.RecipeRagIndexingService;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRagRepository;
import fr.uge.forkeat.service.port.RagSearchPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public final class RagSearchAdapter implements RagSearchPort {

  private static final Logger log = LoggerFactory.getLogger(RagSearchAdapter.class);

  private final EmbeddingModel embeddingModel;
  private final RecipeRagRepository recipeRagRepository;

  @Value("${app.rag.embedding.timeout-seconds:10}")
  private int embeddingTimeoutSeconds;

  public RagSearchAdapter(EmbeddingModel embeddingModel, RecipeRagRepository recipeRagRepository) {
    this.embeddingModel = embeddingModel;
    this.recipeRagRepository = recipeRagRepository;
  }

  @Override
  public List<UUID> findSimilarRecipeIds(String userQuery, int topK) {
    float[] queryVector;
    try {
      var future = CompletableFuture.supplyAsync(() -> embeddingModel.embed(userQuery));
      queryVector = future.get(embeddingTimeoutSeconds, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      log.warn("[RAG] Embedding timeout ({}s), returning empty results", embeddingTimeoutSeconds);
      return List.of();
    } catch (Exception e) {
      log.warn("[RAG] Embedding unavailable, returning empty results: {}", e.getMessage());
      return List.of();
    }
    return recipeRagRepository.findTopKSimilar(RecipeRagIndexingService.toVectorString(queryVector), topK);
  }
}
