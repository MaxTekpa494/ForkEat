package fr.uge.forkeat.presentation.response;

import java.util.Objects;

public record ForbiddenResponse(String description) implements HttpResponse<Void> {

    public ForbiddenResponse {
        Objects.requireNonNull(description);
    }

    @Override
    public boolean success() {
        return false;
    }

    @Override
    public HttpStatusCode statusCode() {
        return HttpStatusCode.FORBIDDEN;
    }

    @Override
    public String message() {
        return HttpStatusCode.FORBIDDEN.getMessage() + " : " + description;
    }
}
