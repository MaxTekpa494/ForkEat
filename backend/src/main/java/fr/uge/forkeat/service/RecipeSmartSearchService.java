package fr.uge.forkeat.service;

import fr.uge.forkeat.service.model.recipe.RecipeUserInteraction;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipeSummary;
import fr.uge.forkeat.service.model.recipe.projection.RecipeCounts;
import fr.uge.forkeat.service.model.recipe.projection.RecipeSummary;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.port.RagModerationPort;
import fr.uge.forkeat.service.port.RagSearchPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
@Transactional(readOnly = true)
public class RecipeSmartSearchService {

  private final RagModerationPort moderationPort;
  private final RagSearchPort ragSearchPort;
  private final RecipePersistence recipePersistence;
  private final AuthenticationPort authPort;
  private final WalletService walletService;

  @Value("${app.rag.top-k:10}")
  private int topK;

  public RecipeSmartSearchService(RagModerationPort moderationPort, RagSearchPort ragSearchPort,
                                  RecipePersistence recipePersistence, AuthenticationPort authPort,
                                  WalletService walletService) {
    this.moderationPort = moderationPort;
    this.ragSearchPort = ragSearchPort;
    this.recipePersistence = recipePersistence;
    this.authPort = authPort;
    this.walletService = walletService;
  }

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  public List<PersonalizedRecipeSummary> search(UUID userId, String userQuery) {
    walletService.debitForSmartSearch(userId);
    moderationPort.assertSafe(userQuery);
    var recipeIds = ragSearchPort.findSimilarRecipeIds(userQuery, topK);
    var summaries = recipePersistence.findSummariesByIds(recipeIds);

    if (summaries.isEmpty()) {
      return List.of();
    }

    var ids = summaries.stream().map(RecipeSummary::id).toList();
    var countsMap = recipePersistence.findRecipeCounts(ids);
    var currentUsername = authPort.extractUsername();
    var interactionsMap = currentUsername != null
            ? recipePersistence.findUserRecipeInteractions(ids, currentUsername)
            : Map.<UUID, RecipeUserInteraction>of();

    return summaries.stream()
            .map(s -> new PersonalizedRecipeSummary(
                    s,
                    countsMap.getOrDefault(s.id(), RecipeCounts.ZERO),
                    interactionsMap.getOrDefault(s.id(), RecipeUserInteraction.NONE)
            ))
            .toList();
  }
}
