package fr.uge.forkeat.presentation.rest.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import fr.uge.forkeat.service.WalletService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StripeWebhookController.class, properties = "stripe.webhook.secret=whsec_fake123")
@AutoConfigureMockMvc(addFilters = false)
class StripeWebhookControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @Autowired
    public StripeWebhookControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void handleStripeEvent_ShouldProcessPayment_WhenSignatureIsValid() throws Exception {
        var payload = "{}";
        var sigHeader = "valid_signature";
        var userId = UUID.randomUUID();
        var stripeTxId = "cs_test_123";

        var mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn(stripeTxId);
        when(mockSession.getAmountTotal()).thenReturn(5000L);
        when(mockSession.getMetadata()).thenReturn(Map.of("userId", userId.toString()));

        var mockDeserializer = mock(EventDataObjectDeserializer.class);
        when(mockDeserializer.getObject()).thenReturn(Optional.of(mockSession));

        var mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("checkout.session.completed");
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);

        try (MockedStatic<Webhook> mockedWebhook = Mockito.mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(mockEvent);

            mockMvc.perform(post("/wallet/webhooks/stripe")
                            .content(payload)
                            .header("Stripe-Signature", sigHeader)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        verify(walletService).processPaymentConfirmation(userId, 5000L, stripeTxId);
    }

    @Test
    void handleStripeEvent_ShouldReturn400_WhenSignatureInvalid() throws Exception {
        try (MockedStatic<Webhook> mockedWebhook = Mockito.mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenThrow(new SignatureVerificationException("Invalid signature", "sig_header"));

            mockMvc.perform(post("/wallet/webhooks/stripe")
                            .content("{}")
                            .header("Stripe-Signature", "invalid_sig"))
                    .andExpect(status().isBadRequest());
        }
    }
}