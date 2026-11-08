package com.boilerplate.domain.exception;

/**
 * A domain rule was violated by an otherwise well-formed request (e.g. "client is
 * revoked", "token expired"). Maps to HTTP 422. Domain and application code must throw
 * this — never a raw {@link IllegalArgumentException} / {@link IllegalStateException} —
 * for business conditions, so the HTTP contract stays explicit.
 */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
