package com.boilerplate.application;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;

/** Shared paginated response envelope. JSON is camelCase like the rest of the internal API. */
public record PagedResponse<T>(List<T> data, int page, int perPage, long count, int totalPages) {
    public static <E, T> PagedResponse<T> from(Page<E> pageData, Function<E, T> mapper) {
        return new PagedResponse<>(
                pageData.getContent().stream().map(mapper).collect(Collectors.toList()),
                pageData.getNumber() + 1,
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages());
    }
}
