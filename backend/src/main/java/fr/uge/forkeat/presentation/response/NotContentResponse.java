package fr.uge.forkeat.presentation.response;

public record NotContentResponse() implements HttpResponse<Void> {

  @Override
  public boolean success() {
    return true;
  }

  @Override
  public HttpStatusCode statusCode() {
    return HttpStatusCode.NO_CONTENT;
  }

  @Override
  public String message() {
    return HttpStatusCode.NO_CONTENT.getMessage();
  }
}
