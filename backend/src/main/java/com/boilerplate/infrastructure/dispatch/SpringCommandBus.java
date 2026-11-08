package com.boilerplate.infrastructure.dispatch;

import com.boilerplate.application.dispatch.Command;
import com.boilerplate.application.dispatch.CommandBus;
import com.boilerplate.application.dispatch.CommandHandler;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Spring adapter that resolves one registered application handler for each command class. */
@Component
public class SpringCommandBus implements CommandBus {

    private final Map<Class<?>, CommandHandler<?, ?>> handlers;

    public SpringCommandBus(List<CommandHandler<?, ?>> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        CommandHandler::commandType, Function.identity(), (left, right) -> {
                            throw new IllegalStateException("Multiple handlers registered for "
                                    + left.commandType().getName());
                        }));
    }

    @Override
    public <R> R dispatch(Command<R> command) {
        CommandHandler<?, ?> handler = handlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalArgumentException(
                    "No command handler registered for " + command.getClass().getName());
        }
        return invoke(handler, command);
    }

    @SuppressWarnings("unchecked")
    private static <C extends Command<R>, R> R invoke(CommandHandler<?, ?> handler, Command<R> command) {
        return ((CommandHandler<C, R>) handler).handle((C) command);
    }
}
