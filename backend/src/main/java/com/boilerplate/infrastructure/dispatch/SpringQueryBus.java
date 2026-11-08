package com.boilerplate.infrastructure.dispatch;

import com.boilerplate.application.dispatch.Query;
import com.boilerplate.application.dispatch.QueryBus;
import com.boilerplate.application.dispatch.QueryHandler;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Spring adapter that resolves one registered application handler for each query class. */
@Component
public class SpringQueryBus implements QueryBus {

    private final Map<Class<?>, QueryHandler<?, ?>> handlers;

    public SpringQueryBus(List<QueryHandler<?, ?>> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(QueryHandler::queryType, Function.identity(), (left, right) -> {
                    throw new IllegalStateException("Multiple handlers registered for "
                            + left.queryType().getName());
                }));
    }

    @Override
    public <R> R dispatch(Query<R> query) {
        QueryHandler<?, ?> handler = handlers.get(query.getClass());
        if (handler == null) {
            throw new IllegalArgumentException(
                    "No query handler registered for " + query.getClass().getName());
        }
        return invoke(handler, query);
    }

    @SuppressWarnings("unchecked")
    private static <Q extends Query<R>, R> R invoke(QueryHandler<?, ?> handler, Query<R> query) {
        return ((QueryHandler<Q, R>) handler).handle((Q) query);
    }
}
