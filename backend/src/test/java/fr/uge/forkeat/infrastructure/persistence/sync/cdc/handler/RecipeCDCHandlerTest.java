package fr.uge.forkeat.infrastructure.persistence.sync.cdc.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.uge.forkeat.infrastructure.persistence.sync.cdc.RecipeNodeClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeCDCHandlerTest {

    @Mock private RecipeNodeClient recipeNodeClient;
    @InjectMocks private RecipeCDCHandler recipeCDCHandler;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void handle_ShouldMergeRecipe_WhenOperationIsCreate() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("op", "c");
        ObjectNode after = payload.putObject("after");
        after.put("id", "recipe-123");
        after.put("title", "Tarte aux pommes");
        after.put("author_id", "author-456");
        recipeCDCHandler.handle("c", payload);
        verify(recipeNodeClient).mergeRecipe("recipe-123", "Tarte aux pommes", "author-456", "c");
    }

    @Test
    void handle_ShouldCreateVariantRelationship_WhenParentIdIsPresent() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("op", "c");
        ObjectNode after = payload.putObject("after");
        after.put("id", "recipe-123");
        after.put("title", "Variante Tarte");
        after.put("author_id", "author-456");
        after.put("parent_id", "parent-789");
        recipeCDCHandler.handle("c", payload);
        verify(recipeNodeClient).mergeRecipe("recipe-123", "Variante Tarte", "author-456", "c");
        verify(recipeNodeClient).createVariantRelationship("recipe-123", "parent-789");
    }

    @Test
    void handle_ShouldDeleteRecipe_WhenOperationIsDelete_AndNoSuperLikes() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode before = payload.putObject("before");
        before.put("id", "recipe-123");
        when(recipeNodeClient.hasSuperLikedRelationships("recipe-123")).thenReturn(false);
        recipeCDCHandler.handle("d", payload);
        verify(recipeNodeClient).reassignVariantParent("recipe-123");
        verify(recipeNodeClient).deleteRecipeNode("recipe-123");
        verify(recipeNodeClient, never()).reassignToSystemEarnings(anyString());
        verify(recipeNodeClient, never()).markDeletedAt(anyString(), any(Instant.class));
    }

    @Test
    void handle_ShouldReassignToSystem_WhenOperationIsDelete_AndHasSuperLikes() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode before = payload.putObject("before");
        before.put("id", "recipe-123");
        when(recipeNodeClient.hasSuperLikedRelationships("recipe-123")).thenReturn(true);
        recipeCDCHandler.handle("d", payload);
        verify(recipeNodeClient).deleteSocialRelationships("recipe-123");
        verify(recipeNodeClient).reassignToSystemEarnings("recipe-123");
        verify(recipeNodeClient).markDeletedAt(eq("recipe-123"), any(Instant.class));
        verify(recipeNodeClient, never()).deleteRecipeNode(anyString());
        verify(recipeNodeClient, never()).reassignVariantParent(anyString());
    }

    @Test
    void handle_ShouldDoNothing_WhenPayloadIsMissingAfterBlock_ForCreate() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("op", "c");
        recipeCDCHandler.handle("c", payload);
        verifyNoInteractions(recipeNodeClient);
    }

    @Test
    void handle_ShouldHandleNullFields_ForCreate() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("op", "c");
        ObjectNode after = payload.putObject("after");
        after.put("id", "recipe-123");
        recipeCDCHandler.handle("c", payload);
        verify(recipeNodeClient).mergeRecipe("recipe-123", null, null, "c");
        verify(recipeNodeClient, never()).createVariantRelationship(anyString(), anyString());
    }

    @Test
    void handle_ShouldNotCreateVariant_WhenParentIdIsNull() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("op", "c");
        ObjectNode after = payload.putObject("after");
        after.put("id", "recipe-123");
        after.putNull("parent_id");
        recipeCDCHandler.handle("c", payload);
        verify(recipeNodeClient).mergeRecipe(anyString(), any(), any(), eq("c"));
        verify(recipeNodeClient, never()).createVariantRelationship(anyString(), anyString());
    }

    @Test
    void handle_ShouldDoNothing_WhenIdIsNull() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("op", "c");
        ObjectNode after = payload.putObject("after");
        after.putNull("id");
        recipeCDCHandler.handle("c", payload);
        verifyNoInteractions(recipeNodeClient);
    }
}