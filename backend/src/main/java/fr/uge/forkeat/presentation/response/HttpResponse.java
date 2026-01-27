package fr.uge.forkeat.presentation.response;

import com.fasterxml.jackson.annotation.JsonInclude;

public interface HttpResponse<T> {
    boolean success();
    HttpStatusCode statusCode();
    String message();
}
