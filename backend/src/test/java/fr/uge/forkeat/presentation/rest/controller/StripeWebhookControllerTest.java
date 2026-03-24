package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.presentation.external.StripeWebhook;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.DuplicateTransactionException;
import fr.uge.forkeat.service.exception.StripEventException;
import fr.uge.forkeat.service.exception.WalletNotFoundException;
import fr.uge.forkeat.service.external.PaymentGateway;
import fr.uge.forkeat.service.model.webhook.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StripeWebhook.class, properties = "stripe.webhook.secret=whsec_fake123")
@AutoConfigureMockMvc(addFilters = false)
class StripeWebhookControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private AuthenticationPort authPort;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @Autowired
    public StripeWebhookControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void handleStripeEvent_ShouldProcessPayment_WhenSignatureIsValid() throws Exception {
        var userId = UUID.randomUUID();
        var stripeTxId = "cs_test_123";

        when(paymentGateway.parseWebhookEvent(anyString(), anyString(), anyString()))
                .thenReturn(new CheckoutSessionCompletedEvent(stripeTxId, userId, 5000L, "paid"));

        mockMvc.perform(post("/api/wallet/webhooks/stripe")
                        .content("{}")
                        .header("Stripe-Signature", "valid_signature")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(walletService).processPaymentConfirmation(userId, 5000L, stripeTxId);
    }

    @Test
    void handleStripeEvent_ShouldReturn400_WhenSignatureInvalid() throws Exception {
        when(paymentGateway.parseWebhookEvent(anyString(), anyString(), anyString()))
                .thenThrow(new StripEventException("Invalid Stripe signature"));

        mockMvc.perform(post("/api/wallet/webhooks/stripe")
                        .content("{}")
                        .header("Stripe-Signature", "invalid_sig")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleStripeEvent_ShouldReturn200_WhenDuplicateTransaction() throws Exception {
        var userId = UUID.randomUUID();
        var stripeTxId = "cs_duplicate";

        when(paymentGateway.parseWebhookEvent(anyString(), anyString(), anyString()))
                .thenReturn(new CheckoutSessionCompletedEvent(stripeTxId, userId, 5000L, "paid"));

        doThrow(new DuplicateTransactionException(stripeTxId))
                .when(walletService).processPaymentConfirmation(eq(userId), eq(5000L), eq(stripeTxId));

        mockMvc.perform(post("/api/wallet/webhooks/stripe")
                        .content("{}")
                        .header("Stripe-Signature", "valid_sig")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void handleStripeEvent_ShouldReturn200_WhenWalletNotFound() throws Exception {
        var userId = UUID.randomUUID();
        var stripeTxId = "cs_no_wallet";

        when(paymentGateway.parseWebhookEvent(anyString(), anyString(), anyString()))
                .thenReturn(new CheckoutSessionCompletedEvent(stripeTxId, userId, 5000L, "paid"));

        doThrow(new WalletNotFoundException(userId))
                .when(walletService).processPaymentConfirmation(eq(userId), eq(5000L), eq(stripeTxId));

        mockMvc.perform(post("/api/wallet/webhooks/stripe")
                        .content("{}")
                        .header("Stripe-Signature", "valid_sig")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void handleStripeEvent_ShouldReturn200_WhenSessionIsNull() throws Exception {
        when(paymentGateway.parseWebhookEvent(anyString(), anyString(), anyString()))
                .thenReturn(new UnknownWebhookEvent("checkout.session.completed#invalid"));

        mockMvc.perform(post("/api/wallet/webhooks/stripe")
                        .content("{}")
                        .header("Stripe-Signature", "valid_sig")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(walletService, never()).processPaymentConfirmation(any(), anyLong(), anyString());
    }

    @Test
    void handleStripeEvent_ShouldIgnore_WhenEventTypeIsNotCheckout() throws Exception {
        when(paymentGateway.parseWebhookEvent(anyString(), anyString(), anyString()))
                .thenReturn(new UnknownWebhookEvent("payment_intent.succeeded"));

        mockMvc.perform(post("/api/wallet/webhooks/stripe")
                        .content("{}")
                        .header("Stripe-Signature", "valid_sig")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(walletService, never()).processPaymentConfirmation(any(), anyLong(), anyString());
    }

    @Test
    void handleStripeEvent_ShouldCallLinkAndConfirmPayout_WhenPendingTxIdInMetadata() throws Exception {
        var transferId = "tr_simulated_abc123";
        var pendingTxId = UUID.randomUUID();

        when(paymentGateway.parseWebhookEvent(anyString(), anyString(), anyString()))
                .thenReturn(new TransferCreatedEvent(transferId, pendingTxId));

        mockMvc.perform(post("/api/wallet/webhooks/stripe")
                        .content("{}")
                        .header("Stripe-Signature", "valid_signature")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(walletService, times(1)).linkAndConfirmPayout(transferId, pendingTxId);
        verify(walletService, never()).processPayoutConfirmation(eq(transferId), eq(true));
    }

    @Test
    void handleStripeEvent_ShouldFallbackToProcessPayoutConfirmation_WhenNoPendingTxIdInMetadata() throws Exception {
        var transferId = "tr_legacy_abc123";

        when(paymentGateway.parseWebhookEvent(anyString(), anyString(), anyString()))
                .thenReturn(new TransferCreatedEvent(transferId, null));

        mockMvc.perform(post("/api/wallet/webhooks/stripe")
                        .content("{}")
                        .header("Stripe-Signature", "valid_signature")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(walletService, times(1)).processPayoutConfirmation(transferId, true);
        verify(walletService, never()).linkAndConfirmPayout(anyString(), any(UUID.class));
    }
}