package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.infrastructure.config.JpaConfig;
import fr.uge.forkeat.infrastructure.config.Neo4jConfig;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.AllergenEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.IngredientEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.AllergenRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.IngredientRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.RecipeRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.recipe.*;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.neo4j.test.autoconfigure.AutoConfigureDataNeo4j;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureDataNeo4j
@Import({RecipePersistenceAdapter.class, Neo4jConfig.class, JpaConfig.class})
@Transactional("transactionManager")
class RecipePersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    private final RecipePersistenceAdapter adapter;

    private final RecipeRepository recipeRepository;

    private final UserRepository userRepository;

    private final AllergenRepository allergenRepository;

    private final IngredientRepository ingredientRepository;


    private UserEntity savedAuthor;

    private Instant now;

    @Autowired
    public RecipePersistenceAdapterIntegrationTest(RecipePersistenceAdapter adapter, RecipeRepository recipeRepository, UserRepository userRepository,
                                                   AllergenRepository allergenRepository, IngredientRepository ingredientRepository) {
        this.adapter = adapter;
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.allergenRepository = allergenRepository;
        this.ingredientRepository = ingredientRepository;
    }

    @BeforeEach
    void setUp() {
        var author = new UserEntity();
        author.setUsername("chef_integration");
        author.setEmail("chef@integration.com");
        author.setFirstName("Chef");
        author.setLastName("Integration");
        author.setPassword("password123");
        author.setRole(UserRole.MEMBER);
        author.setStatus(UserStatus.ACTIVE);
        author.setAuthMode(AuthMode.LOCAL);
        savedAuthor = userRepository.save(author);

        now = Instant.now();
    }

    @Nested
    class Save {

        @Test
        void shouldPersistRecipeInDatabase() {
            var recipe = createRecipe(UUID.randomUUID(), "Tarte aux pommes", RecipeStatus.DRAFT);

            var saved = adapter.save(recipe);

            assertNotNull(saved);
            assertEquals("Tarte aux pommes", saved.title());

            var fromDb = recipeRepository.findById(saved.id());
            assertTrue(fromDb.isPresent());
            assertEquals("Tarte aux pommes", fromDb.get().getTitle());
        }

        @Test
        void shouldPersistRecipeWithIngredients() {
            var ingredient = new IngredientEntity("Farine", "Céréale", false);
            ingredientRepository.save(ingredient);

            var recipe = new Recipe(
                    UUID.randomUUID(), "Gateau", "Un bon gateau", null, "chef_integration",
                    60, null, RecipeStatus.DRAFT,
                    List.of(new RecipeStep(1, "Melanger")),
                    List.of(new RecipeIngredient("Farine", 250.0, "g")),
                    List.of(), List.of(), now, now
            );

            var saved = adapter.save(recipe);

            var fromDb = recipeRepository.findById(saved.id());
            assertTrue(fromDb.isPresent());
            assertEquals(1, fromDb.get().getIngredients().size());
            assertEquals("Farine", fromDb.get().getIngredients().getFirst().getIngredient().getName());
        }

        @Test
        void shouldPersistRecipeWithAllergens() {
            var allergen = new AllergenEntity("Gluten", AllergenSeverity.HIGH);
            var savedAllergen = allergenRepository.save(allergen);

            var recipe = new Recipe(
                    UUID.randomUUID(), "Pain", "Du bon pain", null, "chef_integration",
                    120, null, RecipeStatus.DRAFT, List.of(), List.of(),
                    List.of(new Allergen(savedAllergen.getId(), "Gluten", AllergenSeverity.HIGH)),
                    List.of(), now, now
            );

            var saved = adapter.save(recipe);

            var fromDb = recipeRepository.findById(saved.id());
            assertTrue(fromDb.isPresent());
            assertEquals(1, fromDb.get().getAllergens().size());
            assertEquals("Gluten", fromDb.get().getAllergens().getFirst().getAllergen().getName());
        }

        @Test
        void shouldPersistRecipeWithParent() {
            var parentRecipe = createRecipe(UUID.randomUUID(), "Recette originale", RecipeStatus.PUBLISHED);
            var savedParent = adapter.save(parentRecipe);

            var variantRecipe = new Recipe(
                    UUID.randomUUID(), "Variante", "Une variante", savedParent.id(), "chef_integration",
                    30, null, RecipeStatus.DRAFT, List.of(), List.of(), List.of(), List.of(), now, now
            );

            var savedVariant = adapter.save(variantRecipe);

            assertEquals(savedParent.id(), savedVariant.parentId());
            assertTrue(savedVariant.isVariant());

            var fromDb = recipeRepository.findById(savedVariant.id());
            assertTrue(fromDb.isPresent());
            assertNotNull(fromDb.get().getParent());
            assertEquals(savedParent.id(), fromDb.get().getParent().getId());
        }

        @Test
        void shouldUpdateExistingRecipe() {
            var recipeId = UUID.randomUUID();
            var original = createRecipe(recipeId, "Titre original", RecipeStatus.DRAFT);
            adapter.save(original);

            var updated = new Recipe(
                    recipeId, "Titre modifie", "Summary modifie", null, "chef_integration",
                    45, "https://image.com/new.jpg", RecipeStatus.PUBLISHED,
                    List.of(), List.of(), List.of(), List.of(), now, now
            );
            var result = adapter.save(updated);

            assertEquals("Titre modifie", result.title());
            assertEquals(RecipeStatus.PUBLISHED, result.status());

            var fromDb = recipeRepository.findById(recipeId);
            assertTrue(fromDb.isPresent());
            assertEquals("Titre modifie", fromDb.get().getTitle());
        }
    }

    @Nested
    class FindById {

        @Test
        void shouldReturnPersistedRecipe() {
            var recipe = createRecipe(UUID.randomUUID(), "Quiche Lorraine", RecipeStatus.PUBLISHED);
            var saved = adapter.save(recipe);

            var found = adapter.findById(saved.id());

            assertTrue(found.isPresent());
            assertEquals("Quiche Lorraine", found.get().title());
            assertEquals(RecipeStatus.PUBLISHED, found.get().status());
        }

        @Test
        void shouldReturnEmptyForNonExistentId() {
            var found = adapter.findById(UUID.randomUUID());

            assertTrue(found.isEmpty());
        }
    }

    @Nested
    class FindByStatus {

        @Test
        void shouldReturnOnlyMatchingRecipes() {
            var initialPublished = adapter.findByStatus(RecipeStatus.PUBLISHED);
            var initialDrafts = adapter.findByStatus(RecipeStatus.DRAFT);
            adapter.save(createRecipe(UUID.randomUUID(), "Draft 1", RecipeStatus.DRAFT));
            adapter.save(createRecipe(UUID.randomUUID(), "Draft 2", RecipeStatus.DRAFT));
            adapter.save(createRecipe(UUID.randomUUID(), "Published", RecipeStatus.PUBLISHED));

            var drafts = adapter.findByStatus(RecipeStatus.DRAFT);
            var published = adapter.findByStatus(RecipeStatus.PUBLISHED);

            assertEquals(initialDrafts.size() + 2, drafts.size());
            assertTrue(drafts.stream().allMatch(r -> r.status() == RecipeStatus.DRAFT));
            assertEquals(initialPublished.size() + 1, published.size());
            assertEquals(RecipeStatus.PUBLISHED, published.getFirst().status());
        }
    }

    @Nested
    class FindByAuthorId {

        @Test
        void shouldReturnAuthorRecipes() {
            adapter.save(createRecipe(UUID.randomUUID(), "Recette 1", RecipeStatus.DRAFT));
            adapter.save(createRecipe(UUID.randomUUID(), "Recette 2", RecipeStatus.PUBLISHED));

            var recipes = adapter.findByAuthorId(savedAuthor.getId());

            assertEquals(2, recipes.size());
        }

        @Test
        void shouldReturnEmptyForUnknownAuthor() {
            adapter.save(createRecipe(UUID.randomUUID(), "Recette", RecipeStatus.DRAFT));

            var recipes = adapter.findByAuthorId(UUID.randomUUID());

            assertTrue(recipes.isEmpty());
        }
    }

    @Nested
    class DeleteById {

        @Test
        void shouldRemoveRecipeFromDatabase() {
            var recipe = createRecipe(UUID.randomUUID(), "A supprimer", RecipeStatus.DRAFT);
            var saved = adapter.save(recipe);

            adapter.deleteById(saved.id());

            var found = recipeRepository.findById(saved.id());
            assertTrue(found.isEmpty());
        }
    }

    @Nested
    class FindRecipeSummaries {

        @Test
        void shouldReturnPublishedRecipesWithZeroLikes() {
            adapter.save(createRecipe(UUID.randomUUID(), "Recette publiée", RecipeStatus.PUBLISHED));

            var result = adapter.findRecipeSummaries("chef_integration", RecipeStatus.PUBLISHED, 10, 0);

            assertEquals(1, result.items().size());
            var item = result.items().getFirst();
            assertEquals("Recette publiée", item.title());
        }

        @Test
        void shouldNotReturnDraftWhenFilteredOnPublished() {
            adapter.save(createRecipe(UUID.randomUUID(), "Recette brouillon", RecipeStatus.DRAFT));

            var result = adapter.findRecipeSummaries("chef_integration", RecipeStatus.PUBLISHED, 10, 0);

            assertTrue(result.items().isEmpty());
        }

        @Test
        void shouldReturnDraftWhenFilteredOnDraft() {
            adapter.save(createRecipe(UUID.randomUUID(), "Recette brouillon", RecipeStatus.DRAFT));

            var result = adapter.findRecipeSummaries("chef_integration", RecipeStatus.DRAFT, 10, 0);

            assertEquals(1, result.items().size());
        }

        @Test
        void shouldReturnEmptyForUserWithNoRecipes() {
            var result = adapter.findRecipeSummaries("utilisateur_inexistant", RecipeStatus.PUBLISHED, 10, 0);

            assertTrue(result.items().isEmpty());
            assertEquals(0L, result.total());
        }

        @Test
        void shouldPaginateResults() {
            for (int i = 1; i <= 5; i++) {
                adapter.save(createRecipe(UUID.randomUUID(), "Recette " + i, RecipeStatus.PUBLISHED));
            }

            var firstPage = adapter.findRecipeSummaries("chef_integration", RecipeStatus.PUBLISHED, 3, 0);
            var secondPage = adapter.findRecipeSummaries("chef_integration", RecipeStatus.PUBLISHED, 3, 1);

            assertEquals(3, firstPage.items().size());
            assertEquals(5L, firstPage.total());
            assertEquals(2, secondPage.items().size());
        }
    }

    @Nested
    class FindUserRecipeInteractions {

        @Test
        void shouldReturnFalseWhenUserHasNotInteracted() {
            var saved = adapter.save(createRecipe(UUID.randomUUID(), "Recette", RecipeStatus.PUBLISHED));

            var result = adapter.findUserRecipeInteractions(List.of(saved.id()), "other_user");

            var interaction = result.getOrDefault(saved.id(), RecipeUserInteraction.NONE);
            assertFalse(interaction.likedByCurrentUser());
            assertFalse(interaction.superLikedByCurrentUser());
        }

        @Test
        void shouldReturnEmptyMapForEmptyList() {
            var result = adapter.findUserRecipeInteractions(List.of(), "other_user");

            assertTrue(result.isEmpty());
        }
    }

    private Recipe createRecipe(UUID id, String title, RecipeStatus status) {
        return new Recipe(
                id, title, "Summary for " + title, null, "chef_integration",
                30, null, status, List.of(), List.of(), List.of(), List.of(), now, now
        );
    }
}