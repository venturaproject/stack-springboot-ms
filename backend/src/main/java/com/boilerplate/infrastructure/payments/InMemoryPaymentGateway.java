package com.boilerplate.infrastructure.payments;

import com.boilerplate.application.payments.PaymentAuthorization;
import com.boilerplate.application.payments.PaymentAuthorizationRequest;
import com.boilerplate.application.payments.PaymentGatewayPort;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stand-in payment provider so the fulfillment saga runs end-to-end with no external
 * account. Replace with a Stripe/Adyen adapter — the saga does not change.
 *
 * <p>Behaviour worth keeping in a real adapter: it is <b>idempotent</b> on the request's
 * {@code idempotencyKey} (a retried authorize returns the first authorization, never a
 * second hold). For demos and tests it <b>declines</b> any amount whose major units end in
 * the digit 3 (3.00, 13.50, 123.99 …) — a deterministic way to exercise the compensation path.
 *
 * <p>Registered as {@code "paymentGatewayAdapter"} — {@link ResilientPaymentGateway} is the
 * {@code @Primary} bean the saga actually injects, and it decorates this one. Swapping in a
 * Stripe/Adyen adapter means registering it under the same name; the resilience wrapper and
 * the saga do not move.
 */
@Component("paymentGatewayAdapter")
@Slf4j
public class InMemoryPaymentGateway implements PaymentGatewayPort {

    private final Map<String, String> authorizationsByKey = new ConcurrentHashMap<>();

    @Override
    public PaymentAuthorization authorize(PaymentAuthorizationRequest request) {
        String existing = authorizationsByKey.get(request.idempotencyKey());
        if (existing != null) {
            return PaymentAuthorization.approved(existing);
        }
        if ((request.amountMinorUnits() / 100) % 10 == 3) {
            log.info(
                    "payment declined for order {} (amount {} {})",
                    request.orderId(),
                    request.amountMinorUnits(),
                    request.currency());
            return PaymentAuthorization.declined("card_declined");
        }
        String authorizationId = "auth_" + UUID.randomUUID();
        authorizationsByKey.put(request.idempotencyKey(), authorizationId);
        log.info(
                "payment authorized {} for order {} (amount {} {})",
                authorizationId,
                request.orderId(),
                request.amountMinorUnits(),
                request.currency());
        return PaymentAuthorization.approved(authorizationId);
    }

    @Override
    public void voidAuthorization(String authorizationId) {
        authorizationsByKey.values().remove(authorizationId);
        log.info("payment authorization {} voided", authorizationId);
    }
}
