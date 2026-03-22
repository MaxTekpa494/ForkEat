package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.InsufficientFundsException;
import fr.uge.forkeat.service.exception.WalletNotFoundException;
import fr.uge.forkeat.service.model.recipe.RecipeUserInteraction;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipeSummary;
import fr.uge.forkeat.service.model.recipe.projection.RecipeCounts;
import fr.uge.forkeat.service.model.recipe.projection.RecipeSummary;
import fr.uge.forkeat.service.model.transaction.Transaction;
import fr.uge.forkeat.service.model.transaction.TransactionStatus;
import fr.uge.forkeat.service.model.transaction.TransactionType;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import fr.uge.forkeat.service.persistence.SmartSearchConfigPersistence;
import fr.uge.forkeat.service.persistence.UserPersistence;
import fr.uge.forkeat.service.persistence.WalletPersistence;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.port.RagModerationPort;
import fr.uge.forkeat.service.port.RagSearchPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Service
@Transactional(readOnly = true)
public class RecipeSmartSearchService {

  private final RagModerationPort moderationPort;
  private final RagSearchPort ragSearchPort;
  private final RecipePersistence recipePersistence;
  private final UserPersistence userPersistence;
  private final AuthenticationPort authPort;
  private final WalletPersistence walletPersistence;
  private final SmartSearchConfigPersistence configPersistence;
  private final TransactionTemplate debitTxTemplate;

  public RecipeSmartSearchService(RagModerationPort moderationPort, RagSearchPort ragSearchPort,
                                  RecipePersistence recipePersistence, AuthenticationPort authPort,
                                  WalletPersistence walletPersistence, UserPersistence userPersistance,
                                  SmartSearchConfigPersistence configPersistence,
                                  PlatformTransactionManager transactionManager) {
    this.moderationPort = moderationPort;
    this.ragSearchPort = ragSearchPort;
    this.recipePersistence = recipePersistence;
    this.authPort = authPort;
    this.walletPersistence = walletPersistence;
    this.userPersistence = userPersistance;
    this.configPersistence = configPersistence;

    this.debitTxTemplate = new TransactionTemplate(requireNonNull(transactionManager));
    this.debitTxTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    this.debitTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.debitTxTemplate.setTimeout(10);
  }

  public List<PersonalizedRecipeSummary> search(String userQuery) {
    var config = configPersistence.get();
    var currentUser = authPort.extractUsername();
    var user = userPersistence.findByUsername(currentUser).orElseThrow(() -> new IllegalStateException("User not found"));
    debitTxTemplate.execute(status -> {
      var wallet = walletPersistence.loadWalletWithLock(user.id())
              .orElseThrow(() -> new WalletNotFoundException(user.id()));
      if (wallet.balance() < config.cost()) {
        throw new InsufficientFundsException(wallet.balance(), config.cost());
      }
      walletPersistence.saveWallet(wallet.debit(config.cost()));
      walletPersistence.saveTransaction(new Transaction(
              UUID.randomUUID(), wallet.id(), null, config.cost(),
              TransactionType.SMART_SEARCH, Instant.now(), null, TransactionStatus.SUCCEEDED));
      return null;
    });

    moderationPort.assertSafe(userQuery);
    var recipeIds = ragSearchPort.findSimilarRecipeIds(userQuery, config.topK());
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
