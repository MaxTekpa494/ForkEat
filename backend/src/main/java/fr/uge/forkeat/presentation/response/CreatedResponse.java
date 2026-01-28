package fr.uge.forkeat.presentation.response;

public record CreatedResponse<T>(T resource) implements HttpResponse<T> {
    @Override
    public boolean success() {
        return true;
    }

    @Override
    public HttpStatusCode statusCode() {
        return HttpStatusCode.CREATED;
    }

    @Override
    public String message() {
        return HttpStatusCode.CREATED.getMessage();
    }
}