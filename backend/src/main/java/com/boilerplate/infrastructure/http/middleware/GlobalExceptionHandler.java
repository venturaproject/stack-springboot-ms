package com.boilerplate.infrastructure.http.middleware;

import com.boilerplate.domain.exception.AuthenticationFailedException;
import com.boilerplate.domain.exception.BusinessRuleViolationException;
import com.boilerplate.domain.exception.DomainException;
import com.boilerplate.domain.exception.DuplicateEntityException;
import com.boilerplate.domain.exception.EntityNotFoundException;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.Nullable;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The one place an exception becomes an HTTP response. Every body is an RFC 7807
 * {@code application/problem+json} document: {@code type}, {@code title}, {@code status},
 * {@code detail}, plus {@code traceId} (correlate with logs) and, for validation failures,
 * an {@code errors} map keyed by field.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} means the framework's own exceptions
 * (unreadable body, type mismatch, missing parameter, unsupported media type, 404) already
 * come back as problem documents; the handlers here add the domain layer and a couple of
 * Spring Security cases that reach method security.
 *
 * <p>Business conditions throw a typed {@link DomainException}. Value-object constructors
 * still reject malformed input with a raw {@code IllegalArgumentException} (a bad SKU, a
 * malformed email) — that is a genuine 400, so it keeps a handler; {@code IllegalStateException}
 * maps to 409. Anything else unhandled is a 500, logged.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ObjectProvider<Tracer> tracer;

    public GlobalExceptionHandler(ObjectProvider<Tracer> tracer) {
        this.tracer = tracer;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateEntityException.class)
    public ProblemDetail handleDuplicate(DuplicateEntityException ex) {
        return problem(HttpStatus.CONFLICT, "duplicate", "Resource already exists", ex.getMessage());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ProblemDetail handleAuthFailed(AuthenticationFailedException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "authentication-failed", "Authentication failed", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        return problem(
                HttpStatus.UNAUTHORIZED, "authentication-failed", "Authentication failed", "Invalid credentials");
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleViolationException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "business-rule", "Business rule violation", ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleStaleWrite(OptimisticLockingFailureException ex) {
        return problem(
                HttpStatus.CONFLICT,
                "stale-write",
                "Resource was modified concurrently",
                "This resource changed since you loaded it — reload and try again");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(
                HttpStatus.FORBIDDEN,
                "access-denied",
                "Access denied",
                "You do not have permission to perform this action");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail body = problem(
                HttpStatus.BAD_REQUEST, "validation-error", "Validation failed", "One or more parameters are invalid");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        body.setProperty("errors", errors);
        return body;
    }

    /** Fallback for any other domain exception — an unmapped one is a 400, not a 500. */
    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex) {
        return problem(HttpStatus.BAD_REQUEST, "domain-error", "Request rejected", ex.getMessage());
    }

    /** Value-object constructors reject malformed input this way (bad SKU, bad email). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-value", "Invalid value", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        return problem(HttpStatus.CONFLICT, "illegal-state", "Operation not allowed in this state", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error",
                "Internal server error",
                "An unexpected error occurred");
    }

    // ── Enrich the framework's bean-validation problem with a field map ────────────

    @Override
    @org.jspecify.annotations.Nullable
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = problem(
                HttpStatus.BAD_REQUEST, "validation-error", "Validation failed", "One or more fields are invalid");
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage());
        }
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    private ProblemDetail problem(HttpStatus status, String typeSlug, String title, @Nullable String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail == null ? title : detail);
        body.setType(URI.create("urn:problem-type:" + typeSlug));
        body.setTitle(title);
        String traceId = currentTraceId();
        if (traceId != null) {
            body.setProperty("traceId", traceId);
        }
        return body;
    }

    @Nullable
    private String currentTraceId() {
        Tracer t = tracer.getIfAvailable();
        if (t == null) {
            return null;
        }
        var span = t.currentSpan();
        return span == null ? null : span.context().traceId();
    }
}
