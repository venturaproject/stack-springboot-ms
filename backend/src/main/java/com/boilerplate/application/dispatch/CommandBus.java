package com.boilerplate.application.dispatch;

/** Application boundary for dispatching a command to its single handler. */
public interface CommandBus {

    <R> R dispatch(Command<R> command);
}
