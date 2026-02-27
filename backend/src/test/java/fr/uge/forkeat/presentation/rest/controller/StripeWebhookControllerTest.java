package fr.uge.forkeat.presentation.rest.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Payout;
import com.stripe.model.Transfer;
import com.stripe.model.checkout.Session;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.presentation.external.StripeWebhook;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.DuplicateTransactionException;
import fr.uge.forkeat.service.exception.StripEventException;
import fr.uge.forkeat.service.exception.WalletNotFoundException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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

    private Event mockCheckoutEvent(UUID userId, String stripeTxId, Long amount) {
        var mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn(stripeTxId);
        when(mockSession.getAmountTotal()).thenReturn(amount);
        when(mockSession.getMetadata()).thenReturn(Map.of("userId", userId.toString()));
        when(mockSession.getPaymentStatus()).thenReturn("paid");

        var mockDeserializer = mock(EventDataObjectDeserializer.class);
        when(mockDeserializer.getObject()).thenReturn(Optional.of(mockSession));

        var mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("checkout.session.completed");
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);

        return mockEvent;
    }

    // Helper for Transfer events (platform-level) with pendingTxId in metadata
    private Event mockTransferEvent(String transferId, UUID pendingTxId) {
        var mockTransfer = mock(Transfer.class);
        when(mockTransfer.getId()).thenReturn(transferId);
        when(mockTransfer.getMetadata()).thenReturn(
                pendingTxId != null ? Map.of("pendingTxId", pendingTxId.toString()) : null);

        var mockDeserializer = mock(EventDataObjectDeserializer.class);
        when(mockDeserializer.getObject()).thenReturn(Optional.of(mockTransfer));

        var mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("transfer.created");
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);

        return mockEvent;
    }

    // Helper for Payout events (Connect account level, used for payout.failed)
    private Event mockPayoutEvent(String payoutId, String eventType, String failureMessage) {
        var mockPayout = mock(Payout.class);
        when(mockPayout.getId()).thenReturn(payoutId);
        when(mockPayout.getFailureMessage()).thenReturn(failureMessage);
        when(mockPayout.getMetadata()).thenReturn(Map.of("transferId", "tr_ref_" + payoutId));

        var mockDeserializer = mock(EventDataObjectDeserializer.class);
        when(mockDeserializer.getObject()).thenReturn(Optional.of(mockPayout));

        var mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn(eventType);
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);

        return mockEvent;
    }

    @Test
    void handleStripeEvent_ShouldProcessPayment_WhenSignatureIsValid() throws Exception {
        var userId = UUID.randomUUID();
        var stripeTxId = "cs_test_123";

        var mockEvent = mockCheckoutEvent(userId, stripeTxId, 5000L);
        when(paymentGateway.initEvent(anyString(), anyString(), anyString()))
                .thenReturn(mockEvent);

        mockMvc.perform(post("/wallet/webhooks/stripe")
                        .content("{}")
                        .header("Stripe-Signature", "valid_signature")
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

    @Test
    void handleStripeEvent_ShouldReturn200_WhenDuplicateTransaction() throws Exception {
        var userId = UUID.randomUUID();
        var stripeTxId = "cs_duplicate";

        var mockEvent = mockCheckoutEvent(userId, stripeTxId, 5000L);
        when(paymentGateway.initEvent(anyString(), anyString(), anyString()))
                .thenReturn(mockEvent);

        doThrow(new DuplicateTransactionException(stripeTxId))
                .when(walletService).processPaymentConfirmation(eq(userId), eq(5000L), eq(stripeTxId));

        mockMvc.perform(post("/wallet/webhooks/stripe")
                        .content("{}")
                        .header("Stripe-Signature", "valid_sig")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void handleStripeEvent_ShouldReturn200_WhenWalletNotFound() throws Exception {
        var userId = UUID.randomUUID();
        var stripeTxId = "cs_no_wallet";

        var mockEvent = mockCheckoutEvent(userId, stripeTxId, 5000L);
        when(paymentGateway.initEvent(anyString(), anyString(), anyString()))
                .thenReturn(mockEvent);

        doThrow(new WalletNotFoundException(userId))
                .when(walletService).processPaymentConfirmation(eq(userId), eq(5000L), eq(stripeTxId));

        mockMvc.perform(post("/wallet/webhooks/stripe")
                        .content("{}")
                        .header("Stripe-Signature", "valid_sig")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

//    @Test
//    void handleStripeEvent_ShouldReturn500_WhenUnexpectedException() throws Exception {
//        var userId = UUID.randomUUID();
//        var stripeTxId = "cs_error";
//
//        var mockEvent = mockCheckoutEvent(userId, stripeTxId, 5000L);
//        when(paymentGateway.initEvent(anyString(), anyString(), anyString()))
//                .thenReturn(mockEvent);
//
//        doThrow(new RuntimeException("DB connection lost"))
//                .when(walletService).processPaymentConfirmation(eq(userId), eq(5000L), eq(stripeTxId));
//
//        mockMvc.perform(post("/wallet/webhooks/stripe")
//                        .content("{}")
//                        .header("Stripe-Signature", "valid_sig")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isInternalServerError());
//    }

    @Test
    void handleStripeEvent_ShouldReturn200_WhenSessionIsNull() throws Exception {
        var mockDeserializer = mock(EventDataObjectDeserializer.class);
        when(mockDeserializer.getObject()).thenReturn(Optional.empty());

        var mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("checkout.session.completed");
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);

        when(paymentGateway.initEvent(anyString(), anyString(), anyString()))
                .thenReturn(mockEvent);

        mockMvc.perform(post("/wallet/webhooks/stripe")
                        .content("{}")
                        .header("Stripe-Signature", "valid_sig")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(walletService, never()).processPaymentConfirmation(any(), anyLong(), anyString());
    }

    @Test
    void handleStripeEvent_ShouldIgnore_WhenEventTypeIsNotCheckout() throws Exception {
        var mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

        when(paymentGateway.initEvent(anyString(), anyString(), anyString()))
                .thenReturn(mockEvent);

        mockMvc.perform(post("/wallet/webhooks/stripe")
                        .content("{}")
                        .header("Stripe-Signature", "valid_sig")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(walletService, never()).processPaymentConfirmation(any(), anyLong(), anyString());
    }

    // --- New Tests for Payout Events ---

    @Test
    void handleStripeEvent_ShouldCallLinkAndConfirmPayout_WhenPendingTxIdInMetadata() throws Exception {
        var transferId = "tr_simulated_abc123";
        var pendingTxId = UUID.randomUUID();
        var mockEvent = mockTransferEvent(transferId, pendingTxId);
        when(paymentGateway.initEvent(anyString(), anyString(), anyString())).thenReturn(mockEvent);

        mockMvc.perform(post("/wallet/webhooks/stripe")
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
        var mockEvent = mockTransferEvent(transferId, null);
        when(paymentGateway.initEvent(anyString(), anyString(), anyString())).thenReturn(mockEvent);

        mockMvc.perform(post("/wallet/webhooks/stripe")
                        .content("{}")
                        .header("Stripe-Signature", "valid_signature")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(walletService, times(1)).processPayoutConfirmation(transferId, true);
        verify(walletService, never()).linkAndConfirmPayout(anyString(), any(UUID.class));
    }

//    @Test
//    void handleStripeEvent_ShouldProcessPayoutFailed() throws Exception {
//        var payoutId = "po_failed_123";
//        var failureMessage = "Bank account closed";
//        var mockEvent = mockPayoutEvent(payoutId, "payout.failed", failureMessage);
//        when(paymentGateway.initEvent(anyString(), anyString(), anyString())).thenReturn(mockEvent);
//
//        mockMvc.perform(post("/wallet/webhooks/stripe")
//                        .content("{}")
//                        .header("Stripe-Signature", "valid_signature")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk());
//
//        verify(walletService, times(1)).processPayoutConfirmation(payoutId, false, failureMessage);
//    }
//
//    @Test
//    void handleStripeEvent_ShouldReturn500_WhenPayoutConfirmationFails() throws Exception {
//        var payoutId = "po_error_123";
//        var mockEvent = mockPayoutEvent(payoutId, "payout.succeeded", null);
//        when(paymentGateway.initEvent(anyString(), anyString(), anyString())).thenReturn(mockEvent);
//        doThrow(new WithdrawalException("Failed to confirm payout")).when(walletService).processPayoutConfirmation(eq(payoutId), eq(true), eq(null));
//
//        mockMvc.perform(post("/wallet/webhooks/stripe")
//                        .content("{}")
//                        .header("Stripe-Signature", "valid_signature")
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isInternalServerError());
//    }
}