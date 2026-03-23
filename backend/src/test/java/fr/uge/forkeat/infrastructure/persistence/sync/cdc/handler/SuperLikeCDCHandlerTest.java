package fr.uge.forkeat.infrastructure.persistence.sync.cdc.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.uge.forkeat.infrastructure.persistence.sync.cdc.UserNodeClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuperLikeCDCHandlerTest {

    @Mock private UserNodeClient userNodeClient;
    @InjectMocks private SuperLikeCDCHandler superLikeCDCHandler;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void handle_ShouldMergeSuperLike_WhenOperationIsCreate() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode after = payload.putObject("after");
        after.put("id", "id");
        after.put("user_id", "user-123");
        after.put("recipe_id", "recipeId");
        after.put("amount", "100");
        after.put("redist_amount_cents", "60");
        superLikeCDCHandler.handle("c", payload);
        verify(userNodeClient).addSuperLike("id", "user-123", "recipeId", 100, 60);
    }

    @Test
    void handle_ShouldMergeSuperLike_WhenOperationIsRead() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode after = payload.putObject("after");
        after.put("id", "id");
        after.put("user_id", "user-123");
        after.put("recipe_id", "recipeId");
        after.put("amount", "100");
        after.put("redist_amount_cents", "60");
        superLikeCDCHandler.handle("r", payload);
        verify(userNodeClient).addSuperLike("id", "user-123", "recipeId", 100, 60);
    }

    @Test
    void handle_ShouldDoNothing_WhenAfterIsNull() {
        ObjectNode payload = mapper.createObjectNode();
        superLikeCDCHandler.handle("c", payload);
        verifyNoInteractions(userNodeClient);
    }

    @Test
    void handle_ShouldWarnAndDoNothing_WhenOperationIsDelete() {
        ObjectNode payload = mapper.createObjectNode();
        superLikeCDCHandler.handle("d", payload);
        verifyNoInteractions(userNodeClient);
    }

    @Test
    void handle_ShouldWarnAndDoNothing_WhenOperationIsUnknown() {
        ObjectNode payload = mapper.createObjectNode();
        superLikeCDCHandler.handle("unknown_op", payload);
        verifyNoInteractions(userNodeClient);
    }
}