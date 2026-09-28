package fr.uge.forkeat.infrastructure.persistence.postgres.repository;

import fr.uge.forkeat.infrastructure.AbstractIntegrationTest;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.RecipeStep;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class RecipeRepositoryTest extends AbstractIntegrationTest {

  private final RecipeRepository recipeRepository;

  private final UserRepository userRepository;

  private UserEntity savedAuthor;

  @Autowired
  public RecipeRepositoryTest(RecipeRepository recipeRepository, UserRepository userRepository) {
    this.recipeRepository = recipeRepository;
    this.userRepository = userRepository;
  }


  @BeforeEach
  void setUp() {

    var author = new UserEntity();
    author.setUsername("chef_test");
    author.setEmail("chef@example.com");
    author.setFirstName("Chef");
    author.setLastName("Test");
    author.setPassword("password123");
    author.setRole(UserRole.MEMBER);
    author.setStatus(UserStatus.ACTIVE);
    author.setAuthMode(AuthMode.LOCAL);
    savedAuthor = userRepository.save(author);
  }

  private RecipeEntity createRecipe(String title, String source, String externalId, RecipeStatus status) {
    var recipe = new RecipeEntity();
    recipe.setTitle(title);
    recipe.setSummary("Summary for " + title);
    recipe.setStatus(status);
    recipe.setAuthor(savedAuthor);
    recipe.setStepByStepInstructions(List.of(new RecipeStep(1, "First step")));
    return recipe;
  }

  @Test
  void shouldSaveAndFindRecipeById() {
    var recipe = createRecipe("Pasta Carbonara", "internal", null, RecipeStatus.PUBLISHED);
    var saved = recipeRepository.save(recipe);

    var found = recipeRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("Pasta Carbonara", found.get().getTitle());
  }







  @Test
  void shouldFindByStatus() {
    int initialPublishedCount = recipeRepository.findByStatus(RecipeStatus.PUBLISHED).size();
    int initialDraftCount = recipeRepository.findByStatus(RecipeStatus.DRAFT).size();

    recipeRepository.save(createRecipe("Draft Recipe", "internal", null, RecipeStatus.DRAFT));
    recipeRepository.save(createRecipe("Published Recipe 1", "internal", null, RecipeStatus.PUBLISHED));
    recipeRepository.save(createRecipe("Published Recipe 2", "internal", null, RecipeStatus.PUBLISHED));

    var publishedRecipes = recipeRepository.findByStatus(RecipeStatus.PUBLISHED);
    var draftRecipes = recipeRepository.findByStatus(RecipeStatus.DRAFT);
    assertEquals(initialPublishedCount + 2, publishedRecipes.size());
    assertEquals(initialDraftCount + 1, draftRecipes.size());
  }

  @Test
  void shouldFindByAuthorId() {
    recipeRepository.save(createRecipe("Author Recipe 1", "internal", null, RecipeStatus.PUBLISHED));
    recipeRepository.save(createRecipe("Author Recipe 2", "internal", null, RecipeStatus.DRAFT));

    var authorRecipes = recipeRepository.findByAuthorId(savedAuthor.getId());
    assertEquals(2, authorRecipes.size());
  }



  @Test
  void shouldFindByTitleContainingIgnoreCase() {
    int initialPastaCount = recipeRepository.findByTitleContainingIgnoreCase("pasta").size();

    recipeRepository.save(createRecipe("Pasta Carbonara", "internal", null, RecipeStatus.PUBLISHED));
    recipeRepository.save(createRecipe("Spaghetti Bolognese", "internal", null, RecipeStatus.PUBLISHED));
    recipeRepository.save(createRecipe("pasta primavera", "internal", null, RecipeStatus.DRAFT));

    var pastaRecipes = recipeRepository.findByTitleContainingIgnoreCase("pasta");

    assertEquals(initialPastaCount + 2, pastaRecipes.size());
  }

  @Test
  void shouldCheckIfExistsByTitle() {
    recipeRepository.save(createRecipe("Unique Recipe", "internal", null, RecipeStatus.PUBLISHED));

    assertTrue(recipeRepository.existsByTitle("Unique Recipe"));
    assertFalse(recipeRepository.existsByTitle("Non Existent Recipe"));
  }

  @Test
  void shouldFindByStatusIn() {
    var statuses = List.of(RecipeStatus.PUBLISHED, RecipeStatus.DRAFT);
    int initialCount = recipeRepository.findByStatusIn(statuses).size();

    recipeRepository.save(createRecipe("Draft", "internal", null, RecipeStatus.DRAFT));
    recipeRepository.save(createRecipe("Published", "internal", null, RecipeStatus.PUBLISHED));
    recipeRepository.save(createRecipe("Pending", "internal", null, RecipeStatus.PENDING_REVIEW));
    recipeRepository.save(createRecipe("Rejected", "internal", null, RecipeStatus.REJECTED));

    var activeRecipes = recipeRepository.findByStatusIn(statuses);

    assertEquals(initialCount + 2, activeRecipes.size());
  }

  @Test
  void shouldCountByStatus() {
    // Compter les éléments existants
    long initialPublishedCount = recipeRepository.countByStatus(RecipeStatus.PUBLISHED);
    long initialDraftCount = recipeRepository.countByStatus(RecipeStatus.DRAFT);
    long initialRejectedCount = recipeRepository.countByStatus(RecipeStatus.REJECTED);

    recipeRepository.save(createRecipe("Published 1", "internal", null, RecipeStatus.PUBLISHED));
    recipeRepository.save(createRecipe("Published 2", "internal", null, RecipeStatus.PUBLISHED));
    recipeRepository.save(createRecipe("Draft", "internal", null, RecipeStatus.DRAFT));

    assertEquals(initialPublishedCount + 2, recipeRepository.countByStatus(RecipeStatus.PUBLISHED));
    assertEquals(initialDraftCount + 1, recipeRepository.countByStatus(RecipeStatus.DRAFT));
    assertEquals(initialRejectedCount, recipeRepository.countByStatus(RecipeStatus.REJECTED));
  }

  @Test
  void shouldCountByAuthorId() {
    recipeRepository.save(createRecipe("Recipe 1", "internal", null, RecipeStatus.PUBLISHED));
    recipeRepository.save(createRecipe("Recipe 2", "internal", null, RecipeStatus.DRAFT));

    assertEquals(2, recipeRepository.countByAuthorId(savedAuthor.getId()));
  }

  @Test
  void shouldFindByAuthorIdAndStatus() {
    recipeRepository.save(createRecipe("Published Recipe", "internal", null, RecipeStatus.PUBLISHED));
    recipeRepository.save(createRecipe("Draft Recipe", "internal", null, RecipeStatus.DRAFT));

    var publishedByAuthor = recipeRepository.findByAuthorIdAndStatus(savedAuthor.getId(), RecipeStatus.PUBLISHED);
    var draftByAuthor = recipeRepository.findByAuthorIdAndStatus(savedAuthor.getId(), RecipeStatus.DRAFT);

    assertEquals(1, publishedByAuthor.size());
    assertEquals("Published Recipe", publishedByAuthor.getFirst().getTitle());
    assertEquals(1, draftByAuthor.size());
    assertEquals("Draft Recipe", draftByAuthor.getFirst().getTitle());
  }

  @Test
  void shouldFindByAuthorUsername() {
      recipeRepository.save(createRecipe("Recipe 1", "internal", null, RecipeStatus.PUBLISHED));
      recipeRepository.save(createRecipe("Recipe 2", "internal", null, RecipeStatus.DRAFT));
      assertEquals(2, recipeRepository.findByAuthorUsername(savedAuthor.getUsername()).size());
  }

  @Test
  void shouldFindRecipeSummariesByAuthorIdAndStatusByStatusAndAuthor() {
      recipeRepository.save(createRecipe("Tarte aux pommes", "internal", null, RecipeStatus.PUBLISHED));
      recipeRepository.save(createRecipe("Quiche Lorraine", "internal", null, RecipeStatus.PUBLISHED));
      recipeRepository.save(createRecipe("Brouillon", "internal", null, RecipeStatus.DRAFT));

      var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
      var published = recipeRepository.findRecipeSummariesByAuthorIdAndStatus(savedAuthor.getId(), "PUBLISHED", pageable);
      var drafts = recipeRepository.findRecipeSummariesByAuthorIdAndStatus(savedAuthor.getId(), "DRAFT", pageable);

      assertEquals(2, published.getTotalElements());
      assertEquals(1, drafts.getTotalElements());
  }

  @Test
  void shouldFindRecipeSummariesByAuthorIdAndStatusReturnsOnlyCurrentAuthor() {
      var otherAuthor = new UserEntity();
      otherAuthor.setUsername("other_chef");
      otherAuthor.setEmail("other@example.com");
      otherAuthor.setFirstName("Other");
      otherAuthor.setLastName("Chef");
      otherAuthor.setPassword("password");
      otherAuthor.setRole(UserRole.MEMBER);
      otherAuthor.setStatus(UserStatus.ACTIVE);
      otherAuthor.setAuthMode(AuthMode.LOCAL);
      var savedOther = userRepository.save(otherAuthor);

      recipeRepository.save(createRecipe("Ma recette", "internal", null, RecipeStatus.PUBLISHED));

      var recipeOther = new RecipeEntity();
      recipeOther.setTitle("Recette autre");
      recipeOther.setSummary("Summary");
      recipeOther.setStatus(RecipeStatus.PUBLISHED);
      recipeOther.setAuthor(savedOther);
      recipeOther.setStepByStepInstructions(List.of(new RecipeStep(1, "Step")));
      recipeRepository.save(recipeOther);

      var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
      var result = recipeRepository.findRecipeSummariesByAuthorIdAndStatus(savedAuthor.getId(), "PUBLISHED", pageable);

      assertEquals(1, result.getTotalElements());
      assertEquals("Ma recette", result.getContent().getFirst().getTitle());
  }

  @Test
  void shouldFindRecipeSummariesByAuthorIdAndStatusRespectPagination() {
      for (int i = 1; i <= 5; i++) {
          recipeRepository.save(createRecipe("Recette " + i, "internal", null, RecipeStatus.PUBLISHED));
      }

      var firstPage = recipeRepository.findRecipeSummariesByAuthorIdAndStatus(savedAuthor.getId(), "PUBLISHED", org.springframework.data.domain.PageRequest.of(0, 3));
      var secondPage = recipeRepository.findRecipeSummariesByAuthorIdAndStatus(savedAuthor.getId(), "PUBLISHED", org.springframework.data.domain.PageRequest.of(1, 3));

      assertEquals(5, firstPage.getTotalElements());
      assertEquals(3, firstPage.getContent().size());
      assertEquals(2, secondPage.getContent().size());
  }

  @Test
  void shouldCountByAuthorIdGroupByStatus() {
      recipeRepository.save(createRecipe("Published 1", "internal", null, RecipeStatus.PUBLISHED));
      recipeRepository.save(createRecipe("Published 2", "internal", null, RecipeStatus.PUBLISHED));
      recipeRepository.save(createRecipe("Draft 1", "internal", null, RecipeStatus.DRAFT));

      var counts = recipeRepository.countByAuthorIdGroupByStatus(savedAuthor.getId());

      var countMap = counts.stream().collect(java.util.stream.Collectors.toMap(
              fr.uge.forkeat.infrastructure.persistence.postgres.projection.RecipeStatusCount::getStatus,
              fr.uge.forkeat.infrastructure.persistence.postgres.projection.RecipeStatusCount::getCount
      ));
      assertEquals(2L, countMap.get("PUBLISHED"));
      assertEquals(1L, countMap.get("DRAFT"));
      assertNull(countMap.get("REJECTED"));
  }

  @Test
  void shouldCountByAuthorIdGroupByStatusReturnsEmpty() {
      var counts = recipeRepository.countByAuthorIdGroupByStatus(java.util.UUID.randomUUID());

      assertTrue(counts.isEmpty());
  }
}
