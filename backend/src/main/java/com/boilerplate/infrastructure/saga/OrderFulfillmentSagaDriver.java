package com.boilerplate.infrastructure.saga;

import com.boilerplate.domain.exception.InsufficientStockException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Turns the single-step {@link OrderFulfillmentSaga#advance} into a full run. Kept a
 * separate bean so each step call goes through the transactional proxy (a step per
 * transaction); the loop itself holds no transaction. A stock shortage surfaces as an
 * exception from {@code advance} — that becomes the compensation step.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class OrderFulfillmentSagaDriver {

    private static final int MAX_STEPS = 6;

    private final OrderFulfillmentSaga saga;

    void run(UUID orderId, UUID customerId, long amountMinorUnits, String currency) {
        saga.start(orderId, customerId, amountMinorUnits, currency);
        drive(orderId);
    }

    void drive(UUID orderId) {
        for (int step = 0; step < MAX_STEPS; step++) {
            boolean stillRunning;
            try {
                stillRunning = saga.advance(orderId);
            } catch (InsufficientStockException e) {
                saga.compensateStockShortage(orderId, e.getMessage());
                return;
            }
            if (!stillRunning) {
                return;
            }
        }
        log.warn("saga {} for order {} did not settle in {} steps", OrderFulfillmentSaga.SAGA_TYPE, orderId, MAX_STEPS);
    }
}
