package fr.uge.forkeat.presentation.response;

import java.util.Objects;

public record ConflictResponse(String description) implements HttpResponse<Void> {

    public ConflictResponse {
        Objects.requireNonNull(description);
    }

    @Override
    public boolean success() {
        return false;
    }

    @Override
    public HttpStatusCode statusCode() {
        return HttpStatusCode.CONFLICT;
    }

    @Override
    public String message() {
        return HttpStatusCode.CONFLICT.getMessage() + " : " + description;
    }
}
