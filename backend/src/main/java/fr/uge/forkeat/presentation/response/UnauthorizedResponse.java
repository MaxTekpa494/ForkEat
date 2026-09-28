package fr.uge.forkeat.presentation.response;

import java.util.Objects;

public record UnauthorizedResponse(String desciption) implements HttpResponse<Void>{

  public UnauthorizedResponse{
    Objects.requireNonNull(desciption);
  }

  @Override
  public boolean success() {
    return false;
  }

  @Override
  public HttpStatusCode statusCode() {
    return HttpStatusCode.UNAUTHORIZED;
  }

  @Override
  public String message() {
    return HttpStatusCode.UNAUTHORIZED.getMessage() +" : "+desciption;
  }
}
