package fr.uge.forkeat.service.external;

import com.stripe.model.Event;
import fr.uge.forkeat.service.model.PaymentRequest;
import fr.uge.forkeat.service.model.PaymentResponse;

public interface PaymentGateway {

    /**
     * Demande au système de paiement externe (STRIPE) de préparer une session.
     * @param request Les détails
     * @return La réponse contenant l'URL de redirection
     */
    PaymentResponse initiatePayment(PaymentRequest request);
    Event initEvent(String payload, String sigHeader, String endpointSecret);
}