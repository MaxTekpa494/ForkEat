package fr.uge.forkeat.service;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.TransactionEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.PlatformWalletRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.TransactionRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.WalletRepository;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.transaction.TransactionStatus;
import fr.uge.forkeat.service.model.transaction.TransactionType;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.model.wallet.PlatformWalletType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import fr.uge.forkeat.infrastructure.config.Neo4jConfig;
import org.springframework.boot.data.neo4j.test.autoconfigure.AutoConfigureDataNeo4j;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration du batch de redistribution.
 * Neo4j est configuré directement (pas de Debezium).
 * Chaque test est transactionnel et rolled back → les wallets plateforme
 * restent à leur solde initial entre les tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureDataNeo4j
@Import(Neo4jConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional("transactionManager")
class RedistributionServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired RedistributionService redistributionService;
    @Autowired EntityManager entityManager;
    @Autowired Neo4jClient neo4jClient;
    @Autowired WalletRepository walletRepository;
    @Autowired PlatformWalletRepository platformWalletRepository;
    @Autowired TransactionRepository transactionRepository;

    static final String BATCH_MONTH = "2026-03";
    UUID redistributionWalletId;
    UUID earningsWalletId;

    @BeforeAll
    void setupPlatformWallets() {
        // Les wallets plateforme sont créés par la migration Flyway V6 → on récupère leurs IDs.
        redistributionWalletId = platformWalletRepository.findByType(PlatformWalletType.REDISTRIBUTION)
            .orElseThrow(() -> new IllegalStateException("REDISTRIBUTION wallet not found — check Flyway migration"))
            .getWallet().getId();
        earningsWalletId = platformWalletRepository.findByType(PlatformWalletType.EARNINGS)
            .orElseThrow(() -> new IllegalStateException("EARNINGS wallet not found — check Flyway migration"))
            .getWallet().getId();
        // Recharge les soldes à 100 000 pour que les assertions soient reproductibles entre les tests
        walletRepository.findById(redistributionWalletId).ifPresent(w -> {
            w.setBalance(100_000L);
            walletRepository.save(w);
        });
        walletRepository.findById(earningsWalletId).ifPresent(w -> {
            w.setBalance(100_000L);
            walletRepository.save(w);
        });
    }

    @BeforeEach
    void cleanNeo4j() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    @AfterAll
    void cleanup() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Batch, recette de base : wallet crédité et Neo4j mis à jour")
    void fullBatch_baseRecipe_correctWalletsAndNeo4j() {
        var author = persistUser("author_base");
        var recipeId = UUID.randomUUID().toString();
        setupNeo4jBaseRecipe(author.getId().toString(), recipeId, 60L);

        redistributionService.processAllPending(BATCH_MONTH);

        // Wallet auteur : +60
        assertThat(walletRepository.findById(author.getWallet().getId()).orElseThrow().getBalance())
            .isEqualTo(60L);
        // Wallet redistribution : -60
        assertThat(walletRepository.findById(redistributionWalletId).orElseThrow().getBalance())
            .isEqualTo(100_000L - 60L);

        // Transaction Postgres : créée avec les bons paramètres
        var transactions = transactionRepository.findAll().stream()
            .filter(t -> t.getTransactionType() == TransactionType.REDISTRIBUTION)
            .toList();
        assertThat(transactions).hasSize(1);
        var tx = transactions.getFirst();
        assertThat(tx.getAmount()).isEqualTo(60L);
        assertThat(tx.getSourceWallet().getId()).isEqualTo(redistributionWalletId);
        assertThat(tx.getDestinationWallet().getId()).isEqualTo(author.getWallet().getId());
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);

        // Neo4j : REDISTRIBUTION_RECEIVED créé
        var rrAmount = neo4jClient.query("""
            MATCH (:User {id: $authorId})-[rr:REDISTRIBUTION_RECEIVED {batch_month: $month}]->(:Recipe {id: $recipeId})
            RETURN rr.amount_cents
            """)
            .bind(author.getId().toString()).to("authorId")
            .bind(BATCH_MONTH).to("month")
            .bind(recipeId).to("recipeId")
            .fetchAs(Long.class).one();
        assertThat(rrAmount).contains(60L);

        // Neo4j : SUPER_LIKED marqué
        var batchMonth = neo4jClient.query("MATCH ()-[sl:SUPER_LIKED]->(:Recipe {id: $id}) RETURN sl.batch_month")
            .bind(recipeId).to("id")
            .fetchAs(String.class).one();
        assertThat(batchMonth).contains(BATCH_MONTH);
    }

    @Test
    @DisplayName("Batch, variante 2 niveaux : répartition 30 / 15 / 15")
    void fullBatch_2levelChain_correctSplit() {
        var direct = persistUser("author_direct");
        var parent = persistUser("author_parent");
        var base = persistUser("author_base2");

        var recipeId = UUID.randomUUID().toString();
        var parentRecipeId = UUID.randomUUID().toString();
        var baseRecipeId = UUID.randomUUID().toString();

        neo4jClient.query("""
            MATCH (u1:User {id: $directId}), (u2:User {id: $parentId}), (u3:User {id: $baseId})
            CREATE (r:Recipe {id: $r1})<-[:PUBLISHED]-(u1)
            CREATE (rp:Recipe {id: $r2})<-[:PUBLISHED]-(u2)
            CREATE (rb:Recipe {id: $r3})<-[:PUBLISHED]-(u3)
            CREATE (r)-[:IS_VARIANT_OF]->(rp)-[:IS_VARIANT_OF]->(rb)
            CREATE (:User {id: 'sl_sender'})-[:SUPER_LIKED {
                id: $slId, amount: 100, redist_amount_cents: 60,
                date: datetime('2026-03-01T10:00:00Z')
            }]->(r)
            """)
            .bind(direct.getId().toString()).to("directId")
            .bind(parent.getId().toString()).to("parentId")
            .bind(base.getId().toString()).to("baseId")
            .bind(recipeId).to("r1")
            .bind(parentRecipeId).to("r2")
            .bind(baseRecipeId).to("r3")
            .bind(UUID.randomUUID().toString()).to("slId")
            .run();

        redistributionService.processAllPending(BATCH_MONTH);

        assertThat(walletRepository.findById(direct.getWallet().getId()).orElseThrow().getBalance()).isEqualTo(30L);
        assertThat(walletRepository.findById(parent.getWallet().getId()).orElseThrow().getBalance()).isEqualTo(15L);
        assertThat(walletRepository.findById(base.getWallet().getId()).orElseThrow().getBalance()).isEqualTo(15L);

        // Cross-check Neo4j
        var total = neo4jClient.query("""
            MATCH ()-[rr:REDISTRIBUTION_RECEIVED {batch_month: $month}]->() RETURN sum(rr.amount_cents)
            """)
            .bind(BATCH_MONTH).to("month")
            .fetchAs(Long.class).one();
        assertThat(total).contains(60L);
    }

    @Test
    @DisplayName("Idempotence : transaction déjà présente → pas de double crédit")
    void idempotence_transactionAlreadyExists_noDoubleCredit() {
        var author = persistUser("author_idem");
        var authorWalletId = author.getWallet().getId();
        var recipeId = UUID.randomUUID().toString();

        // Simule un crash post-commit : transaction Postgres déjà enregistrée
        var deterministicId = UUID.nameUUIDFromBytes(
            (recipeId + "#" + BATCH_MONTH + "#" + authorWalletId).getBytes(StandardCharsets.UTF_8));
        var redistWallet = walletRepository.findById(redistributionWalletId).orElseThrow();
        var authorWallet = walletRepository.findById(authorWalletId).orElseThrow();
        var existing = new TransactionEntity(redistWallet, authorWallet, 60L, null,
            TransactionType.REDISTRIBUTION, TransactionStatus.SUCCEEDED, Instant.now());
        existing.setId(deterministicId);
        transactionRepository.save(existing);
        // Balances comme si le paiement avait déjà eu lieu
        redistWallet.setBalance(redistWallet.getBalance() - 60L);
        authorWallet.setBalance(60L);
        walletRepository.save(redistWallet);
        walletRepository.save(authorWallet);
        entityManager.flush();

        // SUPER_LIKED sans batch_month (l'écriture Neo4j n'a pas eu lieu)
        setupNeo4jBaseRecipe(author.getId().toString(), recipeId, 60L);

        redistributionService.processAllPending(BATCH_MONTH);

        // Wallet auteur inchangé (60 et pas 120)
        assertThat(walletRepository.findById(authorWalletId).orElseThrow().getBalance()).isEqualTo(60L);
        // Toujours une seule transaction
        assertThat(transactionRepository.findById(deterministicId)).isPresent();
        assertThat(transactionRepository.findAll().stream()
            .filter(t -> t.getTransactionType() == TransactionType.REDISTRIBUTION)
            .count()).isEqualTo(1L);

        // SUPER_LIKED bien marqué malgré le skip
        var batchMonth = neo4jClient.query("MATCH ()-[sl:SUPER_LIKED]->(:Recipe {id: $id}) RETURN sl.batch_month")
            .bind(recipeId).to("id")
            .fetchAs(String.class).one();
        assertThat(batchMonth).contains(BATCH_MONTH);
    }

    @Test
    @DisplayName("Aucun SL → batch termine sans toucher aux wallets")
    void noUnprocessedSls_batchExitsWithNoOperations() {
        var balanceBefore = walletRepository.findById(redistributionWalletId).orElseThrow().getBalance();

        redistributionService.processAllPending(BATCH_MONTH);

        assertThat(walletRepository.findById(redistributionWalletId).orElseThrow().getBalance())
            .isEqualTo(balanceBefore);
        assertThat(transactionRepository.findAll().stream()
            .filter(t -> t.getTransactionType() == TransactionType.REDISTRIBUTION)
            .count()).isZero();
    }

    // --- Helpers ---

    private UserEntity persistUser(String usernamePrefix) {
        var user = createUserEntity(usernamePrefix);
        entityManager.persist(user);
        entityManager.flush();
        neo4jClient.query("CREATE (:User {id: $id})")
            .bind(user.getId().toString()).to("id")
            .run();
        return user;
    }

    private UserEntity createUserEntity(String usernamePrefix) {
        var user = createUserEntityNoWallet(usernamePrefix);
        var wallet = new WalletEntity(0L, user);
        wallet.setId(UUID.randomUUID());
        user.setWallet(wallet);
        return user;
    }

    /** Crée un UserEntity sans wallet — utilisé pour les users plateforme dont le wallet est géré séparément. */
    private UserEntity createUserEntityNoWallet(String usernamePrefix) {
        var user = new UserEntity();
        user.setUsername(usernamePrefix + "_" + System.nanoTime());
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(user.getUsername() + "@test.com");
        user.setPassword("pass");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.LOCAL);
        user.setEmailVerified(true);
        return user;
    }

    private void setupNeo4jBaseRecipe(String authorId, String recipeId, long redistAmountCents) {
        neo4jClient.query("""
            MATCH (u:User {id: $authorId})
            CREATE (r:Recipe {id: $recipeId})<-[:PUBLISHED]-(u)
            CREATE (:User {id: $senderId})-[:SUPER_LIKED {
                id: $slId, amount: 100,
                redist_amount_cents: $redistAmount,
                date: datetime('2026-03-01T10:00:00Z')
            }]->(r)
            """)
            .bind(authorId).to("authorId")
            .bind(recipeId).to("recipeId")
            .bind(UUID.randomUUID().toString()).to("senderId")
            .bind(UUID.randomUUID().toString()).to("slId")
            .bind(redistAmountCents).to("redistAmount")
            .run();
    }
}