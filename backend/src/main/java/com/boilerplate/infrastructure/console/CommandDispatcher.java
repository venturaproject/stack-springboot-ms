package com.boilerplate.infrastructure.console;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("cli")
@Order(100)
@RequiredArgsConstructor
public class CommandDispatcher implements ApplicationRunner {

    private final List<ConsoleCommand> commands;

    @Override
    public void run(ApplicationArguments args) {
        List<String> nonOption = args.getNonOptionArgs();
        if (nonOption.isEmpty()) {
            printHelp();
            return;
        }
        String commandName = nonOption.get(0);
        commands.stream()
                .filter(c -> c.getName().equals(commandName))
                .findFirst()
                .ifPresentOrElse(cmd -> dispatch(cmd, args), () -> {
                    System.err.println("Unknown command: " + commandName);
                    printHelp();
                    throw new ConsoleException("Unknown command: " + commandName, 1);
                });
    }

    private void dispatch(ConsoleCommand command, ApplicationArguments args) {
        try {
            command.execute(args);
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "Command failed";
            System.err.println("Error: " + message);
            throw new ConsoleException(message, 1);
        }
    }

    private void printHelp() {
        System.out.println("Available commands:");
        commands.forEach(c -> System.out.printf("  %-20s  %s%n", c.getName(), c.getDescription()));
    }
}
