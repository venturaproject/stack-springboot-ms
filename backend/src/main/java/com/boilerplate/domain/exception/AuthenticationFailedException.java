package com.boilerplate.domain.exception;

/**
 * Presented credentials or token could not be authenticated. Maps to HTTP 401.
 * The message is deliberately coarse at the edge to avoid leaking which factor failed.
 */
public class AuthenticationFailedException extends DomainException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
