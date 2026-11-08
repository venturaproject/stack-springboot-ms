package com.boilerplate.domain.exception;

public class DuplicateEntityException extends DomainException {

    public DuplicateEntityException(String message) {
        super(message);
    }
}
