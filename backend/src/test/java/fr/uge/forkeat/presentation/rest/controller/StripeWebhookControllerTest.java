package fr.uge.forkeat.presentation.rest.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import fr.uge.forkeat.infrastructure.config.JwtFilter;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.StripEventException;
import fr.uge.forkeat.service.external.PaymentGateway;
import org.junit.jupiter.api.Test;
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

@WebMvcTest(controllers = StripeWebhookRestController.class, properties = "stripe.webhook.secret=whsec_fake123")
@AutoConfigureMockMvc(addFilters = false)
class StripeWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private PaymentGateway paymentGateway;

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

        when(paymentGateway.initEvent(anyString(), anyString(), anyString()))
                .thenReturn(mockEvent);

            mockMvc.perform(post("/wallet/webhooks/stripe")
                            .content(payload)
                            .header("Stripe-Signature", sigHeader)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        verify(walletService).processPaymentConfirmation(userId, 5000L, stripeTxId);
    }



    @Test
    void handleStripeEvent_ShouldReturn400_WhenSignatureInvalid() throws Exception {
        when(paymentGateway.initEvent(anyString(), anyString(), anyString()))
                .thenThrow(new StripEventException("Invalid Stripe signature",
                        new SignatureVerificationException("Invalid signature", "sig_header")));

        mockMvc.perform(post("/wallet/webhooks/stripe")
                        .content("{}")
                        .header("Stripe-Signature", "invalid_sig")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}