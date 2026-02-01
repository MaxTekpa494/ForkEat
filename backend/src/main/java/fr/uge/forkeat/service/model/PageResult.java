package fr.uge.forkeat.service.model;

import java.util.List;

public record PageResult<T>(List<T> items, long total) {

    public PageResult {
        items = List.copyOf(items);
        if (total < 0) {
            throw new IllegalArgumentException("total must be >= 0");
        }
    }
}
