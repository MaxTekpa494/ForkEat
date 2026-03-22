package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.recipe.RejectRecipeRequest;
import fr.uge.forkeat.presentation.dto.recipe.ValidateReportRequest;
import fr.uge.forkeat.presentation.dto.recipe.DismissReportRequest;
import fr.uge.forkeat.presentation.response.ListResponse;
import fr.uge.forkeat.presentation.web.dto.UserModerationRequest;
import fr.uge.forkeat.service.RecipeModerationActionService;
import fr.uge.forkeat.service.RecipeService;
import fr.uge.forkeat.service.RecipeReportService;
import fr.uge.forkeat.service.UserReportService;
import fr.uge.forkeat.service.UserModerationActionService;
import fr.uge.forkeat.service.model.recipe.RecipeModerationAction;
import fr.uge.forkeat.service.model.user.UserModerationActionType;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.Recipe;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.recipe.projection.RecipeReportDetails;
import fr.uge.forkeat.service.model.user.projection.UserReportDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ModeratorRestControllerTest {

  @Mock
  private RecipeService recipeService;
  @Mock
  private AuthenticationPort authPort;
  @Mock
  private RecipeModerationActionService recipeModerationActionService;
  @Mock
  private RecipeReportService recipeReportService;
  @Mock
  private UserReportService userReportService;
  @Mock
  private UserModerationActionService userModerationActionService;

  private ModeratorRestController moderatorController;
  private Instant now;

  @BeforeEach
  void setUp() {
    moderatorController = new ModeratorRestController(authPort, recipeService, recipeModerationActionService, recipeReportService, userReportService, userModerationActionService);
    now = Instant.now();
  }

  @Nested
  class GetPendingRecipes {

    @Test
    void shouldFilterByPendingStatus() {
      var pendingRecipe = createRecipe(UUID.randomUUID(), "En attente", RecipeStatus.PENDING_REVIEW);
      var pageResult = new PageResult<>(List.of(pendingRecipe), 1);

      when(recipeService.getRecipesToModerate(authPort.extractUsername(), 12, 0)).thenReturn(pageResult);

      var response = moderatorController.getPendingRecipes(12, 0);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertInstanceOf(ListResponse.class, response.getBody());
      var listResponse = (ListResponse<?>) response.getBody();
      assertEquals(1, listResponse.resources().size());
      assertEquals(1, listResponse.total());
    }
  }

  @Nested
  class ValidateRecipe {
    @BeforeEach
    void setUpValidate() {
      when(authPort.extractUsername()).thenReturn("moderatorTest");
    }

    @Test
    void ShouldReturnOkWhenRecipeisFound() {
      var recipeId = UUID.randomUUID();
      var moderationAction = new RecipeModerationAction(
              UUID.randomUUID(),
              recipeId,
              UUID.randomUUID(),
              fr.uge.forkeat.service.model.recipe.RecipeModerationActionType.APPROVED,
              "justification",
              Instant.now(),
              Instant.now(),
              null
      );
      when(recipeModerationActionService.moderateRecipe(any())).thenReturn(moderationAction);

      var response = moderatorController.validateRecipe(recipeId);
      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void ShouldReturnErrorWhenRecipeNotFound() {
      var recipeId = UUID.randomUUID();
      when(recipeModerationActionService.moderateRecipe(any())).thenThrow(new NullPointerException("Recipe not found"));

      assertThrows(NullPointerException.class, () -> moderatorController.validateRecipe(recipeId));
    }
  }

  @Nested
  class RejectRecipe {
    @BeforeEach
    void setUpReject() {
      when(authPort.extractUsername()).thenReturn("moderatorTest");
    }

    @Test
    void ShouldReturnOkWhenRecipeisFound() {
      var recipeId = UUID.randomUUID();
      var moderationAction = new RecipeModerationAction(
              UUID.randomUUID(),
              recipeId,
              UUID.randomUUID(),
              fr.uge.forkeat.service.model.recipe.RecipeModerationActionType.REJECTED,
              "justification",
              Instant.now(),
              Instant.now(),
              null
      );
      when(recipeModerationActionService.moderateRecipe(any())).thenReturn(moderationAction);

      var response = moderatorController.rejectRecipe(recipeId, new RejectRecipeRequest("justification"));
      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void ShouldReturnErrorWhenRecipeNotFound() {
      var recipeId = UUID.randomUUID();
      when(recipeModerationActionService.moderateRecipe(any())).thenThrow(new ResourceNotFoundException("Recipe not found"));

      assertThrows(ResourceNotFoundException.class, () -> moderatorController.rejectRecipe(recipeId, new RejectRecipeRequest("justification")));
    }

    @Test
    void ShouldFailOnEmptyJustification() {
      var recipeId = UUID.randomUUID();
      assertThrows(IllegalArgumentException.class, () ->
              moderatorController.rejectRecipe(recipeId, new RejectRecipeRequest(""))
      );
    }
  }

  @Nested
  class GetReportedRecipes {
    @Test
    void shouldReturnReportedRecipes() {
      var details = new RecipeReportDetails(
        UUID.randomUUID(), UUID.randomUUID(), "Tarte aux pommes", "img.jpg",
        "user1", "SPAM", "Justification", now
      );
      var pageResult = new PageResult<>(List.of(details), 1);
      when(authPort.extractUsername()).thenReturn("moderatorTest");
      when(recipeReportService.getReportsToModerate("moderatorTest", 10, 0)).thenReturn(pageResult);
      var response = moderatorController.getReportedRecipes(10, 0);
      assertEquals(HttpStatus.OK, response.getStatusCode());
      var listResponse = (ListResponse<?>) response.getBody();
      assertEquals(1, listResponse.resources().size());
      assertEquals(1, listResponse.total());
      assertEquals("Tarte aux pommes", ((RecipeReportDetails) listResponse.resources().getFirst()).recipeTitle());
      verify(recipeReportService).getReportsToModerate("moderatorTest", 10, 0);
    }

    @Test
    void shouldReturnEmpty_WhenNoReports() {
      when(authPort.extractUsername()).thenReturn("moderatorTest");
      when(recipeReportService.getReportsToModerate("moderatorTest", 10, 0)).thenReturn(new PageResult<>(List.of(), 0));
      var response = moderatorController.getReportedRecipes(10, 0);
      assertEquals(HttpStatus.OK, response.getStatusCode());
      var listResponse = (ListResponse<?>) response.getBody();
      assertTrue(listResponse.resources().isEmpty());
      assertEquals(0, listResponse.total());
    }
  }

  @Nested
  class ValidateReport {
    @Test
    void shouldValidateReport() {
      var reportId = UUID.randomUUID();
      var recipeId = UUID.randomUUID();
      when(authPort.extractUsername()).thenReturn("moderatorTest");
      when(recipeModerationActionService.moderateRecipe(any())).thenReturn(null);
      var request = new ValidateReportRequest(recipeId);
      var response = moderatorController.validateReport(reportId, request);
      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
      verify(recipeModerationActionService).moderateRecipe(any());
    }
  }

  @Nested
  class GetReportedUsers {
    @Test
    void shouldReturnReportedUsers() {
      var reportedId = UUID.randomUUID();
      var details = new UserReportDetails(
        UUID.randomUUID(), reportedId, "reportedUser", "reporterUser", "SPAM", "Justification", now
      );
      var pageResult = new PageResult<>(List.of(details), 1);

      when(authPort.extractUsername()).thenReturn("moderatorTest");
      when(userReportService.getReportsToModerate("moderatorTest", 10, 0)).thenReturn(pageResult);

      var response = moderatorController.getReportedUsers(10, 0);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      var listResponse = (ListResponse<UserReportDetails>) response.getBody();
      assertEquals(1, listResponse.resources().size());
      assertEquals(1, listResponse.total());
      assertEquals(reportedId, listResponse.resources().getFirst().reportedUserId());
      verify(userReportService).getReportsToModerate("moderatorTest", 10, 0);
    }

    @Test
    void shouldReturnEmpty_WhenNoUserReports() {
      when(authPort.extractUsername()).thenReturn("moderatorTest");
      when(userReportService.getReportsToModerate("moderatorTest", 10, 0)).thenReturn(new PageResult<>(List.of(), 0));

      var response = moderatorController.getReportedUsers(10, 0);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      var listResponse = (ListResponse<UserReportDetails>) response.getBody();
      assertTrue(listResponse.resources().isEmpty());
      assertEquals(0, listResponse.total());
    }
  }

  @Nested
  class DismissReport {
    @Test
    void shouldDismissReport() {
      var reportId = UUID.randomUUID();
      var recipeId = UUID.randomUUID();
      when(authPort.extractUsername()).thenReturn("moderatorTest");
      when(recipeModerationActionService.moderateRecipe(any())).thenReturn(null);
      var request = new DismissReportRequest(recipeId, "Justification de rejet");
      var response = moderatorController.dismissReport(reportId, request);
      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
      verify(recipeModerationActionService).moderateRecipe(any());
    }
  }

  @Nested
  class ResolveUserReport {
    @BeforeEach
    void setUpResolve() {
      when(authPort.extractUsername()).thenReturn("moderatorTest");
    }

    @Test
    void shouldSuspendUserAndValidateReport() {
      var reportId = UUID.randomUUID();
      var userId = UUID.randomUUID();
      var request = new fr.uge.forkeat.presentation.web.dto.UserModerationRequest(
        fr.uge.forkeat.service.model.user.UserModerationActionType.SUSPENDED,
        "Justification",
        userId,
        2,
        5
      );
      when(userModerationActionService.moderateUser(any())).thenReturn(null);
      var response = moderatorController.resolveUserReport(reportId, request);
      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
      verify(userModerationActionService).moderateUser(any());
    }

    @Test
    void shouldBanUserAndValidateReport() {
      var reportId = UUID.randomUUID();
      var userId = UUID.randomUUID();
      var request = new fr.uge.forkeat.presentation.web.dto.UserModerationRequest(
        fr.uge.forkeat.service.model.user.UserModerationActionType.BANNED,
        "Justification",
        userId,
        0,
        0
      );
      when(userModerationActionService.moderateUser(any())).thenReturn(null);
      var response = moderatorController.resolveUserReport(reportId, request);
      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
      verify(userModerationActionService).moderateUser(any());
    }

    @Test
    void shouldWarnUserAndValidateReport() {
      var reportId = UUID.randomUUID();
      var userId = UUID.randomUUID();
      var request = new fr.uge.forkeat.presentation.web.dto.UserModerationRequest(
        fr.uge.forkeat.service.model.user.UserModerationActionType.WARNING,
        "Justification",
        userId,
        0,
        0
      );
      when(userModerationActionService.moderateUser(any())).thenReturn(null);
      var response = moderatorController.resolveUserReport(reportId, request);
      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
      verify(userModerationActionService).moderateUser(any());
    }

    @Test
    void shouldDismissUserReport() {
      var reportId = UUID.randomUUID();
      var userId = UUID.randomUUID();
      var request = new fr.uge.forkeat.presentation.web.dto.UserModerationRequest(
        fr.uge.forkeat.service.model.user.UserModerationActionType.DISMISSED,
        "Justification",
        userId,
        0,
        0
      );
      when(userModerationActionService.moderateUser(any())).thenReturn(null);
      var response = moderatorController.resolveUserReport(reportId, request);
      assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
      verify(userModerationActionService).moderateUser(any());
    }

    @Test
    void shouldThrowResourceNotFoundException_WhenUserNotFound() {
      var reportId = UUID.randomUUID();
      var userId = UUID.randomUUID();
      var request = new fr.uge.forkeat.presentation.web.dto.UserModerationRequest(
        fr.uge.forkeat.service.model.user.UserModerationActionType.WARNING,
        "Justification",
        userId,
        0,
        0
      );
      when(userModerationActionService.moderateUser(any())).thenThrow(new fr.uge.forkeat.service.exception.ResourceNotFoundException("User not found"));
      assertThrows(fr.uge.forkeat.service.exception.ResourceNotFoundException.class, () -> moderatorController.resolveUserReport(reportId, request));
    }

    @Test
    void shouldThrowResourceNotFoundException_WhenReportNotFound() {
      var reportId = UUID.randomUUID();
      var userId = UUID.randomUUID();
      var request = new fr.uge.forkeat.presentation.web.dto.UserModerationRequest(
        fr.uge.forkeat.service.model.user.UserModerationActionType.WARNING,
        "Justification",
        userId,
        0,
        0
      );
      when(userModerationActionService.moderateUser(any())).thenThrow(new fr.uge.forkeat.service.exception.ResourceNotFoundException("User report not found"));
      assertThrows(fr.uge.forkeat.service.exception.ResourceNotFoundException.class, () -> moderatorController.resolveUserReport(reportId, request));
    }

    @Test
    void shouldThrowModeratorIsAuthorException_WhenModeratorIsAuthor() {
      var reportId = UUID.randomUUID();
      var userId = UUID.randomUUID();
      var request = new fr.uge.forkeat.presentation.web.dto.UserModerationRequest(
        fr.uge.forkeat.service.model.user.UserModerationActionType.WARNING,
        "Justification",
        userId,
        0,
        0
      );
      when(userModerationActionService.moderateUser(any())).thenThrow(new fr.uge.forkeat.service.exception.ModeratorIsAuthorException());
      assertThrows(fr.uge.forkeat.service.exception.ModeratorIsAuthorException.class, () -> moderatorController.resolveUserReport(reportId, request));
    }
  }

  private Recipe createRecipe(UUID id, String title, RecipeStatus status) {
    return new Recipe(
            id,
            title,
            "Summary for " + title,
            null,
            "chef_test",
            30,
            null,
            status,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            now,
            now
    );
  }
}
