package fr.uge.forkeat.service;

import fr.uge.forkeat.presentation.dto.recipe.RecipeDetailsDTO;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import fr.uge.forkeat.service.port.RagModerationPort;
import fr.uge.forkeat.service.port.RagSearchPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RecipeSmartSearchService {

  private final RagModerationPort moderationPort;
  private final RagSearchPort ragSearchPort;
  private final RecipePersistence recipePersistence;

  @Value("${app.rag.top-k:10}")
  private int topK;


  public RecipeSmartSearchService(RagModerationPort moderationPort, RagSearchPort ragSearchPort, RecipePersistence recipePersistence) {
    this.moderationPort = moderationPort;
    this.ragSearchPort = ragSearchPort;
    this.recipePersistence = recipePersistence;
  }

  public List<Recipe> search(String userQuery) {
    moderationPort.assertSafe(userQuery);
    var recipeIds = ragSearchPort.findSimilarRecipeIds(userQuery, topK);
    return recipeIds.stream() // Voir sii on ne peut pas juste faire une seule requette qui ramene toutes les recettes
            .map(recipePersistence::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
  }


}
