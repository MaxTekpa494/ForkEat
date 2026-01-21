package fr.uge.forkeat.service.external;


import fr.uge.forkeat.presentation.rest.dto.PaymentRequestDto;
import fr.uge.forkeat.presentation.rest.dto.PaymentResponseDto;

public interface PaymentGateway {

    /**
     * Demande au système de paiement externe (STRIPE) de préparer une session.
     * * @param request Les détails
     * @return La réponse contenant l'URL de redirection
     */
    PaymentResponseDto initiatePayment(PaymentRequestDto request);
}