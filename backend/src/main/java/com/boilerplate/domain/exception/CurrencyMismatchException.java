package com.boilerplate.domain.exception;

/** Arithmetic was attempted on two {@code Money} values in different currencies. Maps to HTTP 422. */
public class CurrencyMismatchException extends BusinessRuleViolationException {

    public CurrencyMismatchException(String expected, String actual) {
        super("Currency mismatch: expected " + expected + ", got " + actual);
    }
}
