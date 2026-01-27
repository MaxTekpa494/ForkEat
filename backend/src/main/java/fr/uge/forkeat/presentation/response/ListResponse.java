package fr.uge.forkeat.presentation.response;

import java.util.List;

public record ListResponse<T>(List<T> resources, int total) implements HttpResponse<T>{

    public ListResponse{
        resources = List.copyOf(resources);
        if(total < 0){
            throw new IllegalArgumentException("total < 0 : Impossible");
        }
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
