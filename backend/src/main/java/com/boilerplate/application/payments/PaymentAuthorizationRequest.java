package com.boilerplate.application.payments;

import java.util.UUID;

public record PaymentAuthorizationRequest(
        UUID orderId, UUID customerId, long amountMinorUnits, String currency, String idempotencyKey) {}
