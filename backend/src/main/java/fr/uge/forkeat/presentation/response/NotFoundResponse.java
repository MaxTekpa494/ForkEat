package fr.uge.forkeat.presentation.response;

import java.util.Objects;

public record NotFoundResponse(String description) implements HttpResponse<Void>{

    public NotFoundResponse{
        Objects.requireNonNull(description);
    }
    @Override
    public boolean success() {
        return false;
    }

    @Override
    public HttpStatusCode statusCode() {
        return HttpStatusCode.NOT_FOUND;
    }

    @Override
    public String message() {
        return HttpStatusCode.NOT_FOUND.getMessage() +" : "+ description;
    }
}
