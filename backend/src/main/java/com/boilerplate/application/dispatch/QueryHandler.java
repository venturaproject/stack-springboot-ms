package com.boilerplate.application.dispatch;

/** Handles one query type. Implementations own the read use case and its transaction boundary. */
public interface QueryHandler<Q extends Query<R>, R> {

    Class<Q> queryType();

    R handle(Q query);
}
