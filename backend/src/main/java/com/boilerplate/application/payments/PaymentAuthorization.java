package com.boilerplate.application.payments;

import jakarta.annotation.Nullable;

/**
 * Outcome of an authorization attempt. A decline is an expected business result, not an
 * error — {@code approved == false} with a {@code declineReason}.
 */
public record PaymentAuthorization(@Nullable String authorizationId, boolean approved, @Nullable String declineReason) {
    public static PaymentAuthorization approved(String authorizationId) {
        return new PaymentAuthorization(authorizationId, true, null);
    }

    public static PaymentAuthorization declined(String reason) {
        return new PaymentAuthorization(null, false, reason);
    }
}
