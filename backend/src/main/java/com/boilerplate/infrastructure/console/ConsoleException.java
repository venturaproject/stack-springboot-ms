package com.boilerplate.infrastructure.console;

import org.springframework.boot.ExitCodeGenerator;

public class ConsoleException extends RuntimeException implements ExitCodeGenerator {

    private final int code;

    public ConsoleException(String message, int code) {
        super(message, null, true, false);
        this.code = code;
    }

    @Override
    public int getExitCode() {
        return code;
    }
}
