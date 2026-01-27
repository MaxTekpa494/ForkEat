package fr.uge.forkeat.presentation.response;

public record SuccessResponse() implements HttpResponse<Void> {

  @Override
  public boolean success() {
    return true;
  }

  @Override
  public HttpStatusCode statusCode() {
    return HttpStatusCode.OK;
  }

  @Override
  public String message() {
    return HttpStatusCode.OK.getMessage();
  }
}
