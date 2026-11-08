package com.boilerplate.infrastructure.payments;

import com.boilerplate.application.payments.PaymentAuthorization;
import com.boilerplate.application.payments.PaymentAuthorizationRequest;
import com.boilerplate.application.payments.PaymentGatewayPort;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Resilience wrapper around whatever {@link PaymentGatewayPort} adapter is registered as
 * {@code "paymentGatewayAdapter"}. This is the {@code @Primary} bean, so the fulfillment saga
 * gets circuit breaking, retry with backoff, and a concurrency bulkhead for free — and none
 * of that logic leaks into the adapter or the domain.
 *
 * <p>Execution order (resilience4j default): {@code Retry ( CircuitBreaker ( Bulkhead ( call ) ) )}.
 * A transient fault is retried a few times; sustained failure trips the breaker (fast-fail for
 * {@code wait-duration-in-open-state}); either way the caller gets a graceful
 * {@code declined("gateway_unavailable")} from the fallback, never an exception. Tune the
 * instance {@code payment-gateway} in {@code application.yml}; metrics land on
 * {@code /actuator/prometheus} under {@code resilience4j_*}.
 *
 * <p>A network timeout also belongs here. resilience4j's {@code @TimeLimiter} only wraps
 * {@code CompletableFuture} returns, so a synchronous port either sets the timeout on its own
 * HTTP client (preferred) or exposes an async method.
 *
 * <p>This is the template for <em>any</em> outbound port that leaves the process.
 */
@Component
@Primary
@Slf4j
public class ResilientPaymentGateway implements PaymentGatewayPort {

    private static final String INSTANCE = "payment-gateway";

    private final PaymentGatewayPort delegate;

    public ResilientPaymentGateway(@Qualifier("paymentGatewayAdapter") PaymentGatewayPort delegate) {
        this.delegate = delegate;
    }

    @Retry(name = INSTANCE, fallbackMethod = "authorizeFallback")
    @CircuitBreaker(name = INSTANCE)
    @Bulkhead(name = INSTANCE)
    @Override
    public PaymentAuthorization authorize(PaymentAuthorizationRequest request) {
        return delegate.authorize(request);
    }

    // Invoked reflectively by resilience4j once retries are exhausted or the breaker is open.
    PaymentAuthorization authorizeFallback(PaymentAuthorizationRequest request, Throwable failure) {
        log.warn("payment gateway unavailable for order {} — {}", request.orderId(), failure.toString());
        return PaymentAuthorization.declined("gateway_unavailable");
    }

    @Retry(name = INSTANCE, fallbackMethod = "voidFallback")
    @CircuitBreaker(name = INSTANCE)
    @Bulkhead(name = INSTANCE)
    @Override
    public void voidAuthorization(String authorizationId) {
        delegate.voidAuthorization(authorizationId);
    }

    void voidFallback(String authorizationId, Throwable failure) {
        // A hold we could not release now — surface it for reconciliation rather than losing it.
        log.error(
                "could not void payment authorization {} (needs manual reconciliation) — {}",
                authorizationId,
                failure.toString());
    }
}
