package fr.uge.forkeat.presentation.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;

public interface HttpResponse<T> {
    @JsonProperty boolean success();
    @JsonProperty HttpStatusCode statusCode();
    @JsonProperty String message();

    @JsonProperty default String timestamp() {
        return Instant.now().toString();
    }

  enum HttpStatusCode {
    OK(200, "OK"),
    CREATED(201, "Created"),
    NO_CONTENT(204, "No Content"),

    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    PAYMENT_REQUIRED(402, "Payment Required"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    CONFLICT(409, "Conflict"),
    UNPROCESSABLE_CONTENT(422, "Unprocessable Content"),

    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    private final int code;
    private final String message;

    HttpStatusCode(int code, String message) {
      Objects.requireNonNull(message);
      this.code = code;
      this.message = message;
    }

    public int getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }
  }
}