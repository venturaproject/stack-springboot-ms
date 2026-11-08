package com.boilerplate.application.dispatch;

/** Handles one command type. Implementations own the use case and its transaction boundary. */
public interface CommandHandler<C extends Command<R>, R> {

    Class<C> commandType();

    R handle(C command);
}
