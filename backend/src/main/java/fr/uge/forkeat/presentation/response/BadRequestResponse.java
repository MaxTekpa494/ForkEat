package fr.uge.forkeat.presentation.response;

import java.util.Objects;

public record BadRequestResponse(String description) implements HttpResponse<Void> {

    public BadRequestResponse{
        Objects.requireNonNull(description);
    }
    @Override
    public boolean success() {
        return false;
    }

    @Override
    public HttpStatusCode statusCode() {
        return HttpStatusCode.BAD_REQUEST;
    }

    @Override
    public String message(){
        return HttpStatusCode.BAD_REQUEST.getMessage() +" : "+description;
    }
}
