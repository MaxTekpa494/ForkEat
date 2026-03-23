package fr.uge.forkeat.presentation.response;

import java.util.Objects;

public record PaymentRequiredResponse(String description) implements HttpResponse<Void> {

    public PaymentRequiredResponse {
        Objects.requireNonNull(description);
    }

    @Override
    public boolean success() {
        return false;
    }

    @Override
    public HttpStatusCode statusCode() {
        return HttpStatusCode.PAYMENT_REQUIRED;
    }

    @Override
    public String message() {
        return HttpStatusCode.PAYMENT_REQUIRED.getMessage() + " : " + description;
    }
}
