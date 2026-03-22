package fr.uge.forkeat.infrastructure.persistence.sync.cdc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Neo4jSyncServiceTest {

    @Mock private UserNodeClient userNodeClient;
    @Mock private RecipeNodeClient recipeNodeClient;
    @InjectMocks private Neo4jSyncService neo4jSyncService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void handleUserChange_ShouldMergeUser_WhenOperationIsCreate() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode after = payload.putObject("after");
        after.put("id", "user-123");
        after.put("username", "john_doe");
        after.put("email", "john@test.com");
        neo4jSyncService.handleUserChange("c", payload);
        verify(userNodeClient).mergeUser("user-123", "john_doe", "john@test.com");
    }

    @Test
    void handleUserChange_ShouldMergeUser_WhenOperationIsUpdate() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode after = payload.putObject("after");
        after.put("id", "user-123");
        after.put("username", "john_doe_updated");
        after.put("email", "john@test.com");
        neo4jSyncService.handleUserChange("u", payload);
        verify(userNodeClient).mergeUser("user-123", "john_doe_updated", "john@test.com");
    }

    @Test
    void handleUserChange_ShouldDeleteUser_WhenOperationIsDelete_AndNoSuperLikes() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode before = payload.putObject("before");
        before.put("id", "user-123");
        when(userNodeClient.hasSuperLikedRelationships("user-123")).thenReturn(false);
        neo4jSyncService.handleUserChange("d", payload);
        verify(userNodeClient).reassignRecipesToSystemEarnings("user-123");
        verify(userNodeClient).deleteAllUserRelationships("user-123");
        verify(userNodeClient).deleteUserNode("user-123");
        verify(userNodeClient, never()).markAsDeleted(anyString());
    }

    @Test
    void handleUserChange_ShouldMarkAsDeleted_WhenOperationIsDelete_AndHasSuperLikes() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode before = payload.putObject("before");
        before.put("id", "user-123");
        when(userNodeClient.hasSuperLikedRelationships("user-123")).thenReturn(true);
        neo4jSyncService.handleUserChange("d", payload);
        verify(userNodeClient).reassignRecipesToSystemEarnings("user-123");
        verify(userNodeClient).deleteAllUserRelationships("user-123");
        verify(userNodeClient).markAsDeleted("user-123");
        verify(userNodeClient, never()).deleteUserNode(anyString());
    }

    @Test
    void handleUserChange_ShouldDoNothing_WhenPayloadIsMissingAfterBlock_ForCreate() {
        ObjectNode payload = mapper.createObjectNode();
        neo4jSyncService.handleUserChange("c", payload);
        verifyNoInteractions(userNodeClient);
    }

    @Test
    void handleUserChange_ShouldDoNothing_WhenPayloadIsMissingBeforeBlock_ForDelete() {
        ObjectNode payload = mapper.createObjectNode();
        neo4jSyncService.handleUserChange("d", payload);
        verifyNoInteractions(userNodeClient);
    }

    @Test
    void handleUserChange_ShouldHandleNullFields_ForCreate() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode after = payload.putObject("after");
        after.put("id", "user-123");
        neo4jSyncService.handleUserChange("c", payload);
        verify(userNodeClient).mergeUser("user-123", null, null);
    }

    @Test
    void handleUserChange_ShouldIgnoreUnknownOperation() {
        ObjectNode payload = mapper.createObjectNode();
        neo4jSyncService.handleUserChange("unknown_op", payload);
        verifyNoInteractions(userNodeClient);
    }

    @Test
    void handleRecipeChange_ShouldMergeRecipe_WhenOperationIsCreate() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("op", "c");
        ObjectNode after = payload.putObject("after");
        after.put("id", "recipe-123");
        after.put("title", "Tarte aux pommes");
        after.put("author_id", "author-456");
        neo4jSyncService.handleRecipeChange("c", payload);
        verify(recipeNodeClient).mergeRecipe("recipe-123", "Tarte aux pommes", "author-456", "c", "PUBLISHED");
    }

    @Test
    void handleRecipeChange_ShouldCreateVariantRelationship_WhenParentIdIsPresent() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("op", "c");
        ObjectNode after = payload.putObject("after");
        after.put("id", "recipe-123");
        after.put("title", "Variante Tarte");
        after.put("author_id", "author-456");
        after.put("parent_id", "parent-789");
        neo4jSyncService.handleRecipeChange("c", payload);
        verify(recipeNodeClient).mergeRecipe("recipe-123", "Variante Tarte", "author-456", "c", "PUBLISHED");
        verify(recipeNodeClient).createVariantRelationship("recipe-123", "parent-789");
    }

    @Test
    void handleRecipeChange_ShouldDeleteRecipe_WhenOperationIsDelete_AndNoSuperLikes() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode before = payload.putObject("before");
        before.put("id", "recipe-123");
        when(recipeNodeClient.hasSuperLikedRelationships("recipe-123")).thenReturn(false);
        neo4jSyncService.handleRecipeChange("d", payload);
        verify(recipeNodeClient).reassignVariantParent("recipe-123");
        verify(recipeNodeClient).deleteRecipeNode("recipe-123");
        verify(recipeNodeClient, never()).reassignToSystemEarnings(anyString());
        verify(recipeNodeClient, never()).markAsDeleted(anyString());
    }

    @Test
    void handleRecipeChange_ShouldReassignToSystem_WhenOperationIsDelete_AndHasSuperLikes() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode before = payload.putObject("before");
        before.put("id", "recipe-123");
        when(recipeNodeClient.hasSuperLikedRelationships("recipe-123")).thenReturn(true);
        neo4jSyncService.handleRecipeChange("d", payload);
        verify(recipeNodeClient).deleteSocialRelationships("recipe-123");
        verify(recipeNodeClient).reassignToSystemEarnings("recipe-123");
        verify(recipeNodeClient).markAsDeleted("recipe-123");
        verify(recipeNodeClient, never()).deleteRecipeNode(anyString());
    }

    @Test
    void handleRecipeChange_ShouldDoNothing_WhenPayloadIsMissingAfterBlock_ForCreate() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("op", "c");
        neo4jSyncService.handleRecipeChange("c", payload);
        verifyNoInteractions(recipeNodeClient);
    }

    @Test
    void handleRecipeChange_ShouldHandleNullFields_ForCreate() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("op", "c");
        ObjectNode after = payload.putObject("after");
        after.put("id", "recipe-123");
        neo4jSyncService.handleRecipeChange("c", payload);
        verify(recipeNodeClient).mergeRecipe("recipe-123", null, null, "c", "PUBLISHED");
        verify(recipeNodeClient, never()).createVariantRelationship(anyString(), anyString());
    }

    @Test
    void handleRecipeChange_ShouldNotCreateVariant_WhenParentIdIsNull() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("op", "c");
        ObjectNode after = payload.putObject("after");
        after.put("id", "recipe-123");
        after.putNull("parent_id");
        neo4jSyncService.handleRecipeChange("c", payload);
        verify(recipeNodeClient).mergeRecipe(anyString(), any(), any(), eq("c"), eq("PUBLISHED"));
        verify(recipeNodeClient, never()).createVariantRelationship(anyString(), anyString());
    }

    @Test
    void handleUserChange_ShouldDoNothing_WhenIdIsNull() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode after = payload.putObject("after");
        after.putNull("id");
        neo4jSyncService.handleUserChange("c", payload);
        verifyNoInteractions(userNodeClient);
    }

    @Test
    void handleRecipeChange_ShouldDoNothing_WhenIdIsNull() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("op", "c");
        ObjectNode after = payload.putObject("after");
        after.putNull("id");
        neo4jSyncService.handleRecipeChange("c", payload);
        verifyNoInteractions(recipeNodeClient);
    }

    @Test
    void handleSuperLikeChange_ShouldMergeSuperLike_WhenOperationIsCreate() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode after = payload.putObject("after");
        after.put("id", "id");
        after.put("user_id", "user-123");
        after.put("recipe_id", "recipeId");
        after.put("amount", "100");
        neo4jSyncService.handleSuperLikeChange("c", payload);
        verify(userNodeClient).addSuperLike("user-123", "recipeId", 100);
    }

    @Test
    void handleSuperLikeChange_ShouldMergeSuperLike_WhenOperationIsRead() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode after = payload.putObject("after");
        after.put("id", "id");
        after.put("user_id", "user-123");
        after.put("recipe_id", "recipeId");
        after.put("amount", "100");
        neo4jSyncService.handleSuperLikeChange("r", payload);
        verify(userNodeClient).addSuperLike("user-123", "recipeId", 100);
    }
}
