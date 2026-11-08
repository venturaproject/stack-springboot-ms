package com.boilerplate.application.dispatch;

/** Application boundary for dispatching a query to its single handler. */
public interface QueryBus {

    <R> R dispatch(Query<R> query);
}
