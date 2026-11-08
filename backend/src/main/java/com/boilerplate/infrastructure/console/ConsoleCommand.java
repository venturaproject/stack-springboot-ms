package com.boilerplate.infrastructure.console;

import org.springframework.boot.ApplicationArguments;

public interface ConsoleCommand {

    String getName();

    String getDescription();

    void execute(ApplicationArguments args);
}
