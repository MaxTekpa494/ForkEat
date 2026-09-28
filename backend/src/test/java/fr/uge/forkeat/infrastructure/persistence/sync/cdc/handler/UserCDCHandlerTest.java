package fr.uge.forkeat.infrastructure.persistence.sync.cdc.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.uge.forkeat.infrastructure.persistence.sync.cdc.UserNodeClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCDCHandlerTest {

    @Mock private UserNodeClient userNodeClient;
    @InjectMocks private UserCDCHandler userCDCHandler;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void handle_ShouldMergeUser_WhenOperationIsCreate() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode after = payload.putObject("after");
        after.put("id", "user-123");
        after.put("username", "john_doe");
        after.put("email", "john@test.com");
        userCDCHandler.handle("c", payload);
        verify(userNodeClient).mergeUser("user-123", "john_doe", "john@test.com");
    }

    @Test
    void handle_ShouldMergeUser_WhenOperationIsUpdate() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode after = payload.putObject("after");
        after.put("id", "user-123");
        after.put("username", "john_doe_updated");
        after.put("email", "john@test.com");
        userCDCHandler.handle("u", payload);
        verify(userNodeClient).mergeUser("user-123", "john_doe_updated", "john@test.com");
    }

    @Test
    void handle_ShouldDeleteUser_WhenOperationIsDelete_AndNoRecipeInteractions() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode before = payload.putObject("before");
        before.put("id", "user-123");
        when(userNodeClient.hasRecipeInteractionRelationships("user-123")).thenReturn(false);
        userCDCHandler.handle("d", payload);
        verify(userNodeClient).reassignRecipesToSystemEarnings("user-123");
        verify(userNodeClient).deleteAllUserRelationships("user-123");
        verify(userNodeClient).deleteUserNode("user-123");
        verify(userNodeClient, never()).markAsDeleted(anyString());
        verify(userNodeClient, never()).deleteFollowRelationships(anyString());
    }

    @Test
    void handle_ShouldMarkAsDeleted_WhenOperationIsDelete_AndHasRecipeInteractions() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode before = payload.putObject("before");
        before.put("id", "user-123");
        when(userNodeClient.hasRecipeInteractionRelationships("user-123")).thenReturn(true);
        userCDCHandler.handle("d", payload);
        verify(userNodeClient).reassignRecipesToSystemEarnings("user-123");
        verify(userNodeClient).deleteFollowRelationships("user-123");
        verify(userNodeClient).markAsDeleted("user-123");
        verify(userNodeClient, never()).deleteUserNode(anyString());
        verify(userNodeClient, never()).deleteAllUserRelationships(anyString());
    }

    @Test
    void handle_ShouldDoNothing_WhenPayloadIsMissingAfterBlock_ForCreate() {
        ObjectNode payload = mapper.createObjectNode();
        userCDCHandler.handle("c", payload);
        verifyNoInteractions(userNodeClient);
    }

    @Test
    void handle_ShouldDoNothing_WhenPayloadIsMissingBeforeBlock_ForDelete() {
        ObjectNode payload = mapper.createObjectNode();
        userCDCHandler.handle("d", payload);
        verifyNoInteractions(userNodeClient);
    }

    @Test
    void handle_ShouldHandleNullFields_ForCreate() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode after = payload.putObject("after");
        after.put("id", "user-123");
        userCDCHandler.handle("c", payload);
        verify(userNodeClient).mergeUser("user-123", null, null);
    }

    @Test
    void handle_ShouldIgnoreUnknownOperation() {
        ObjectNode payload = mapper.createObjectNode();
        userCDCHandler.handle("unknown_op", payload);
        verifyNoInteractions(userNodeClient);
    }

    @Test
    void handle_ShouldDoNothing_WhenIdIsNull() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode after = payload.putObject("after");
        after.putNull("id");
        userCDCHandler.handle("c", payload);
        verifyNoInteractions(userNodeClient);
    }
}