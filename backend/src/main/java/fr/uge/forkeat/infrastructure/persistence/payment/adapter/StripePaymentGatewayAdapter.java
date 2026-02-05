package fr.uge.forkeat.infrastructure.persistence.payment.adapter;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import fr.uge.forkeat.service.exception.PaymentException;
import fr.uge.forkeat.service.exception.StripEventException;
import fr.uge.forkeat.service.model.PaymentRequest;
import fr.uge.forkeat.service.model.PaymentResponse;
import fr.uge.forkeat.service.external.PaymentGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class StripePaymentGatewayAdapter implements PaymentGateway {

    @Value("${app.front.url}")
    private String frontUrl; // Temporaire

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        var amountInCents = request.amount();

        //CONSTRUCTION DE LA REQUÊTE STRIPE
        var params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontUrl + "/payment/success")
                .setCancelUrl(frontUrl + "/payment/cancel")
                .setCustomerEmail(request.email())
                .putMetadata("userId", String.valueOf(request.userId()))

                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount(amountInCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Rechargement ForkEat")
                                        .build())
                                .build())
                        .build())
                .build();

        //APPEL A L'API STRIPE
        try {
            var session = Session.create(params);

            return new PaymentResponse(session.getUrl(), session.getId());

        } catch (StripeException e) {
            throw new PaymentException("Erreur lors de la communication avec Stripe", e);
        }
    }

    public Event initEvent(String payload, String sigHeader, String endpointSecret) {
        try {
            return Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            throw new StripEventException("Invalid Stripe signature", e);
        }
    }
}