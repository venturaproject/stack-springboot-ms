package com.boilerplate.infrastructure.saga;

import com.boilerplate.domain.orders.event.OrderPlaced;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Starts the fulfillment saga once an order is actually placed. {@code AFTER_COMMIT} so the
 * saga never runs for an order whose placement rolled back; it then drives synchronously in
 * this thread — swap to {@code @Async} (or trigger off the outbox topic under the {@code
 * kafka} profile) if placement latency matters.
 */
@Component
@RequiredArgsConstructor
class OrderFulfillmentSagaListener {

    private final OrderFulfillmentSagaDriver driver;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(OrderPlaced event) {
        driver.run(event.orderId(), event.customerId(), event.totalAmountMinorUnits(), event.currency());
    }
}
