package fr.uge.forkeat.infrastructure.ai.adapter;

import fr.uge.forkeat.infrastructure.ai.RecipeRagIndexingService;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRagRepository;
import fr.uge.forkeat.service.port.RagSearchPort;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public final class RagSearchAdapter implements RagSearchPort {

  private final EmbeddingModel embeddingModel;
  private final RecipeRagRepository recipeRagRepository;

  public RagSearchAdapter(EmbeddingModel embeddingModel, RecipeRagRepository recipeRagRepository) {
    this.embeddingModel = embeddingModel;
    this.recipeRagRepository = recipeRagRepository;
  }


  @Override
  public List<UUID> findSimilarRecipeIds(String userQuery, int topK) {
    var queryVector = embeddingModel.embed(userQuery);
    return recipeRagRepository.findTopKSimilar(RecipeRagIndexingService.toVectorString(queryVector), topK);
  }
}
