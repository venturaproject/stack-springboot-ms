package com.boilerplate.infrastructure.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.boilerplate.application.payments.PaymentAuthorization;
import com.boilerplate.application.payments.PaymentAuthorizationRequest;
import com.boilerplate.application.payments.PaymentGatewayPort;
import com.boilerplate.support.AbstractIntegrationIT;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * The resilience wrapper (circuit breaker + retry + bulkhead) is applied by Spring AOP, so it
 * needs a real context. The adapter it decorates is mocked to fail on demand.
 */
class ResilientPaymentGatewayIT extends AbstractIntegrationIT {

    @MockitoBean(name = "paymentGatewayAdapter")
    PaymentGatewayPort adapter;

    @Autowired
    PaymentGatewayPort gateway; // the @Primary ResilientPaymentGateway

    @Autowired
    CircuitBreakerRegistry circuitBreakers;

    private static PaymentAuthorizationRequest request() {
        return new PaymentAuthorizationRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                2000,
                "USD",
                UUID.randomUUID().toString());
    }

    @BeforeEach
    void resetBreaker() {
        circuitBreakers.circuitBreaker("payment-gateway").reset();
    }

    @Test
    void a_transient_failure_is_retried_and_then_succeeds() {
        when(adapter.authorize(any()))
                .thenThrow(new RuntimeException("connection reset"))
                .thenThrow(new RuntimeException("connection reset"))
                .thenReturn(PaymentAuthorization.approved("auth_ok"));

        PaymentAuthorization result = gateway.authorize(request());

        assertThat(result.approved()).isTrue();
        assertThat(result.authorizationId()).isEqualTo("auth_ok");
        verify(adapter, times(3)).authorize(any());
    }

    @Test
    void sustained_failure_opens_the_breaker_and_the_caller_gets_a_graceful_decline() {
        when(adapter.authorize(any())).thenThrow(new RuntimeException("gateway 500"));

        for (int i = 0; i < 25; i++) {
            PaymentAuthorization result = gateway.authorize(request());
            assertThat(result.approved()).isFalse();
            assertThat(result.declineReason()).isEqualTo("gateway_unavailable");
        }

        assertThat(circuitBreakers.circuitBreaker("payment-gateway").getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
