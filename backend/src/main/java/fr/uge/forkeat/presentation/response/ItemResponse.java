    package fr.uge.forkeat.presentation.response;

    import java.util.Objects;

    public record ItemResponse<T>(T resource) implements HttpResponse<T> {

        public ItemResponse{
            Objects.requireNonNull(resource);
        }
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
