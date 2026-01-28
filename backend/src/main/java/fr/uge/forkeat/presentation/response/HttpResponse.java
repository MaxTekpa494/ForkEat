package fr.uge.forkeat.presentation.response;


public interface HttpResponse<T> {
    boolean success();
    HttpStatusCode statusCode();
    String message();
}