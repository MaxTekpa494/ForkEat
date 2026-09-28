package fr.uge.forkeat.presentation.response;

import java.util.Objects;

public record UnprocessableContentResponse(String description) implements HttpResponse<Void> {

    public UnprocessableContentResponse {
        Objects.requireNonNull(description);
    }

    @Override
    public boolean success() {
        return false;
    }

    @Override
    public HttpStatusCode statusCode() {
        return HttpStatusCode.UNPROCESSABLE_CONTENT;
    }

    @Override
    public String message() {
        return HttpStatusCode.UNPROCESSABLE_CONTENT.getMessage() + " : " + description;
    }
}
