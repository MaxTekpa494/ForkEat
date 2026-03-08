package fr.uge.forkeat.infrastructure.ai;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeRagEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRagRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRepository;
import fr.uge.forkeat.service.exception.RecipeNotFoundException;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecipeRagIndexingService {

  private final RecipeRepository recipeRepository;
  private final RecipeRagRepository recipeRagRepository;
  private final EmbeddingModel embeddingModel;

  public RecipeRagIndexingService(RecipeRepository recipeRepository, RecipeRagRepository recipeRagRepository, EmbeddingModel embeddingModel) {
    this.recipeRepository = recipeRepository;
    this.recipeRagRepository = recipeRagRepository;
    this.embeddingModel = embeddingModel;
  }

  @Transactional
  public void indexRecipe(UUID recipeId) {
    var recipe = recipeRepository.findById(recipeId)
            .orElseThrow(() -> new RecipeNotFoundException(recipeId));

    var ingredients  = extractIngredients(recipe);
    var dietaries    = extractDietaries(recipe);
    var allergens    = extractAllergens(recipe);
    var documentText = buildDocumentText(recipe, ingredients, dietaries, allergens);

    var vector = embeddingModel.embed(documentText);

    var rag = recipeRagRepository.findByRecipe_Id(recipeId).orElse(new RecipeRagEntity());

    rag.setRecipe(recipe);
    rag.setDocumentText(documentText);
    rag.setTitle(recipe.getTitle());
    rag.setIngredients(ingredients);
    rag.setDietaries(dietaries);
    rag.setAllergens(allergens);
    rag.setReadyInMinutes(recipe.getPreparationMinutes());
    recipeRagRepository.save(rag);

    // voir si on ne peut pas envoyer le float[] directement dans la base
    recipeRagRepository.updateEmbedding(recipe.getId(), toVectorString(vector));
  }

  private String extractIngredients(RecipeEntity recipe) {
    return recipe.getIngredients().stream()
            .map(ri -> ri.getIngredient().getName())
            .collect(Collectors.joining(", "));
  }

  private String extractDietaries(RecipeEntity recipe) {
    return recipe.getDietaries().stream()
            .map(rd -> rd.getDietary().getName())
            .collect(Collectors.joining(", "));
  }

  private String extractAllergens(RecipeEntity recipe) {
    return recipe.getAllergens().stream()
            .map(ra -> ra.getAllergen().getName())
            .collect(Collectors.joining(", "));
  }

  private String buildDocumentText(RecipeEntity recipe, String ingredients,
                                   String dietaries, String allergens) {
    var sb = new StringBuilder();
    sb.append("Titre: ").append(recipe.getTitle()).append("\n");
    if (!ingredients.isBlank())
      sb.append("Ingrédients: ").append(ingredients).append("\n");
    if (!dietaries.isBlank())
      sb.append("Régimes: ").append(dietaries).append("\n");
    if (!allergens.isBlank())
      sb.append("Allergènes: ").append(allergens).append("\n");
    if (recipe.getPreparationMinutes() > 0)
      sb.append("Temps de préparation: ").append(recipe.getPreparationMinutes()).append(" minutes\n");
    if (recipe.getSummary() != null && !recipe.getSummary().isBlank())
      sb.append("Description: ").append(recipe.getSummary());
    return sb.toString().trim();
  }

  public static String toVectorString(float[] vector) {
    var sb = new StringBuilder("[");
    for (int i = 0; i < vector.length; i++) {
      sb.append(vector[i]);
      if (i < vector.length - 1) sb.append(",");
    }
    return sb.append("]").toString();
  }
}
