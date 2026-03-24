package fr.uge.forkeat.service.external;

// Cette classe ne respecte pas l'archi hexa, il y a une dépendence avec
// l'api stripe
import com.stripe.model.Event;
import fr.uge.forkeat.service.model.payment.PaymentRequest;
import fr.uge.forkeat.service.model.payment.PaymentResponse;

public interface PaymentGateway {

    /**
     * Demande au système de paiement externe (STRIPE) de préparer une session.
     * @param request Les détails
     * @return La réponse contenant l'URL de redirection
     */
    PaymentResponse initiatePayment(PaymentRequest request);
    Event initEvent(String payload, String sigHeader, String endpointSecret);
}