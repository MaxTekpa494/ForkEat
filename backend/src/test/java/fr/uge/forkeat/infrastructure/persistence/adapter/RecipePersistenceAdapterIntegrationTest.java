package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Import(RecipePersistenceAdapter.class)
class RecipePersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    private final RecipePersistenceAdapter adapter;

    private final RecipeRepository recipeRepository;

    private final UserRepository userRepository;

    private final AllergenRepository allergenRepository;

    private final IngredientRepository ingredientRepository;

    private UserEntity savedAuthor;
    private Instant now;

    @Autowired
    public RecipePersistenceAdapterIntegrationTest(RecipePersistenceAdapter adapter, RecipeRepository recipeRepository, UserRepository userRepository, AllergenRepository allergenRepository, IngredientRepository ingredientRepository) {
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

    @Test
    void save_shouldPersistRecipeInDatabase() {
        var recipe = createRecipe(UUID.randomUUID(), "Tarte aux pommes", RecipeStatus.DRAFT);

        var saved = adapter.save(recipe);

        assertNotNull(saved);
        assertEquals("Tarte aux pommes", saved.title());

        // Verify it's actually in the database
        var fromDb = recipeRepository.findById(saved.id());
        assertTrue(fromDb.isPresent());
        assertEquals("Tarte aux pommes", fromDb.get().getTitle());
    }

    @Test
    void save_shouldPersistRecipeWithIngredients() {
        var ingredient = new IngredientEntity("Farine", "Céréale", false);
        ingredientRepository.save(ingredient);

        var recipe = new Recipe(
                UUID.randomUUID(), "Gateau", "Un bon gateau", null, "chef_integration",
                60, null, RecipeStatus.DRAFT,
                List.of(new RecipeStep(1, "Melanger")),
                List.of(new RecipeIngredient("Farine", 250.0, "g")),
                List.of(), Map.of(), now, now
        );

        var saved = adapter.save(recipe);

        var fromDb = recipeRepository.findById(saved.id());
        assertTrue(fromDb.isPresent());
        assertEquals(1, fromDb.get().getIngredients().size());
        assertEquals("Farine", fromDb.get().getIngredients().getFirst().getIngredient().getName());
    }

    @Test
    void save_shouldPersistRecipeWithAllergens() {
        var allergen = new AllergenEntity("Gluten", AllergenSeverity.HIGH);
        var savedAllergen = allergenRepository.save(allergen);

        var recipe = new Recipe(
                UUID.randomUUID(), "Pain", "Du bon pain", null, "chef_integration",
                120, null, RecipeStatus.DRAFT, List.of(), List.of(),
                List.of(new Allergen(savedAllergen.getId(), "Gluten", AllergenSeverity.HIGH)),
                Map.of(), now, now
        );

        var saved = adapter.save(recipe);

        var fromDb = recipeRepository.findById(saved.id());
        assertTrue(fromDb.isPresent());
        assertEquals(1, fromDb.get().getAllergens().size());
        assertEquals("Gluten", fromDb.get().getAllergens().getFirst().getAllergen().getName());
    }

    @Test
    void save_shouldPersistRecipeWithParent() {
        var parentRecipe = createRecipe(UUID.randomUUID(), "Recette originale", RecipeStatus.PUBLISHED);
        var savedParent = adapter.save(parentRecipe);

        var variantRecipe = new Recipe(
                UUID.randomUUID(), "Variante", "Une variante", savedParent.id(), "chef_integration",
                30, null, RecipeStatus.DRAFT, List.of(), List.of(), List.of(), Map.of(), now, now
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
    void findById_shouldReturnPersistedRecipe() {
        var recipe = createRecipe(UUID.randomUUID(), "Quiche Lorraine", RecipeStatus.PUBLISHED);
        var saved = adapter.save(recipe);

        var found = adapter.findById(saved.id());

        assertTrue(found.isPresent());
        assertEquals("Quiche Lorraine", found.get().title());
        assertEquals(RecipeStatus.PUBLISHED, found.get().status());
    }

    @Test
    void findById_shouldReturnEmptyForNonExistentId() {
        var found = adapter.findById(UUID.randomUUID());

        assertTrue(found.isEmpty());
    }

    @Test
    void findByStatus_shouldReturnOnlyMatchingRecipes() {
        var initialPublished = adapter.findByStatus("PUBLISHED");
        var initialDrafts = adapter.findByStatus("DRAFT");
        adapter.save(createRecipe(UUID.randomUUID(), "Draft 1", RecipeStatus.DRAFT));
        adapter.save(createRecipe(UUID.randomUUID(), "Draft 2", RecipeStatus.DRAFT));
        adapter.save(createRecipe(UUID.randomUUID(), "Published", RecipeStatus.PUBLISHED));

        var drafts = adapter.findByStatus("DRAFT");
        var published = adapter.findByStatus("PUBLISHED");

        assertEquals(initialDrafts.size() + 2, drafts.size());
        assertTrue(drafts.stream().allMatch(r -> r.status() == RecipeStatus.DRAFT));
        assertEquals(initialPublished.size() + 1, published.size());
        assertEquals(RecipeStatus.PUBLISHED, published.getFirst().status());
    }

    @Test
    void findByAuthorId_shouldReturnAuthorRecipes() {
        adapter.save(createRecipe(UUID.randomUUID(), "Recette 1", RecipeStatus.DRAFT));
        adapter.save(createRecipe(UUID.randomUUID(), "Recette 2", RecipeStatus.PUBLISHED));

        var recipes = adapter.findByAuthorId(savedAuthor.getId());

        assertEquals(2, recipes.size());
    }

    @Test
    void findByAuthorId_shouldReturnEmptyForUnknownAuthor() {
        adapter.save(createRecipe(UUID.randomUUID(), "Recette", RecipeStatus.DRAFT));

        var recipes = adapter.findByAuthorId(UUID.randomUUID());

        assertTrue(recipes.isEmpty());
    }

    @Test
    void deleteById_shouldRemoveRecipeFromDatabase() {
        var recipe = createRecipe(UUID.randomUUID(), "A supprimer", RecipeStatus.DRAFT);
        var saved = adapter.save(recipe);

        adapter.deleteById(saved.id());

        var found = recipeRepository.findById(saved.id());
        assertTrue(found.isEmpty());
    }

    @Test
    void save_shouldUpdateExistingRecipe() {
        var recipeId = UUID.randomUUID();
        var original = createRecipe(recipeId, "Titre original", RecipeStatus.DRAFT);
        adapter.save(original);

        var updated = new Recipe(
                recipeId, "Titre modifie", "Summary modifie", null, "chef_integration",
                45, "https://image.com/new.jpg", RecipeStatus.PUBLISHED,
                List.of(), List.of(), List.of(), Map.of(), now, now
        );
        var result = adapter.save(updated);

        assertEquals("Titre modifie", result.title());
        assertEquals(RecipeStatus.PUBLISHED, result.status());

        var fromDb = recipeRepository.findById(recipeId);
        assertTrue(fromDb.isPresent());
        assertEquals("Titre modifie", fromDb.get().getTitle());
    }

    private Recipe createRecipe(UUID id, String title, RecipeStatus status) {
        return new Recipe(
                id, title, "Summary for " + title, null, "chef_integration",
                30, null, status, List.of(), List.of(), List.of(), Map.of(), now, now
        );
    }
}
