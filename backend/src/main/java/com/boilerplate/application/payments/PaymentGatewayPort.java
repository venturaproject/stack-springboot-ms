package com.boilerplate.application.payments;

/**
 * Outbound port for the payment provider — the one step of order fulfillment that leaves
 * this system (Stripe, Adyen, a bank API). It is deliberately a port with a throwaway
 * in-memory adapter: swapping in a real provider must not touch the saga.
 *
 * <p>{@link #authorize} is idempotent on {@link PaymentAuthorizationRequest#idempotencyKey()}
 * — the saga may call it again after a crash and must get the same authorization back, not a
 * second charge.
 */
public interface PaymentGatewayPort {

    PaymentAuthorization authorize(PaymentAuthorizationRequest request);

    /** Compensation: release a hold taken by {@link #authorize} but never captured. */
    void voidAuthorization(String authorizationId);
}
