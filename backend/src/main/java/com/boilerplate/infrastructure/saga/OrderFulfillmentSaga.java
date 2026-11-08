package com.boilerplate.infrastructure.saga;

import com.boilerplate.application.payments.PaymentAuthorization;
import com.boilerplate.application.payments.PaymentAuthorizationRequest;
import com.boilerplate.application.payments.PaymentGatewayPort;
import com.boilerplate.domain.exception.EntityNotFoundException;
import com.boilerplate.domain.orders.Order;
import com.boilerplate.domain.orders.OrderRepository;
import com.boilerplate.domain.orders.OrderStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration saga for order fulfillment. When an order is placed it drives, one
 * committed step at a time:
 *
 * <pre>
 *   STARTED ──authorize payment──▶ PAYMENT_AUTHORIZED ──reserve stock──▶ STOCK_RESERVED ──▶ COMPLETED
 *      │                                   │
 *      └── declined ──▶ COMPENSATED         └── out of stock ──▶ (void payment) ──▶ COMPENSATED
 * </pre>
 *
 * <p>Every step is one {@code REQUIRES_NEW} transaction that persists the new
 * {@link SagaInstance} state before returning, and every step is idempotent — payment is
 * keyed on the saga id, stock reservation is folded into the same transaction as the
 * {@code STOCK_RESERVED} transition (so a retry never double-reserves), and the order
 * transitions are status-guarded. A crash therefore resumes from the last committed step
 * (see {@code SagaRecovery}) with no double effects.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderFulfillmentSaga {

    static final String SAGA_TYPE = "order-fulfillment";
    static final String STARTED = "STARTED";
    static final String PAYMENT_AUTHORIZED = "PAYMENT_AUTHORIZED";
    static final String STOCK_RESERVED = "STOCK_RESERVED";
    static final String COMPLETED = "COMPLETED";
    static final String COMPENSATED = "COMPENSATED";

    private final SagaInstanceRepository sagaRepository;
    private final OrderRepository orderRepository;
    private final PaymentGatewayPort paymentGateway;
    private final StockReserver stockReserver;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void start(UUID orderId, UUID customerId, long amountMinorUnits, String currency) {
        String payload = write(new State(orderId, customerId, amountMinorUnits, currency, null));
        int inserted = sagaRepository.insertIfAbsent(UUID.randomUUID(), SAGA_TYPE, orderId, STARTED, payload);
        if (inserted == 0) {
            log.debug("saga {} for order {} already started", SAGA_TYPE, orderId);
        }
    }

    /**
     * Runs one forward step. Returns {@code true} if it advanced and is still running.
     * Throws {@link com.boilerplate.domain.exception.InsufficientStockException} out of the
     * reservation step — the driver turns that into {@link #compensateStockShortage}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean advance(UUID orderId) {
        SagaInstance saga = sagaRepository
                .findBySagaTypeAndCorrelationId(SAGA_TYPE, orderId)
                .orElse(null);
        if (saga == null || !saga.isRunning()) {
            return false;
        }
        State state = read(saga.getPayload());
        switch (saga.getState()) {
            case STARTED -> authorizePayment(saga, state);
            case PAYMENT_AUTHORIZED -> {
                stockReserver.reserveAll(orderId);
                saga.transitionTo(STOCK_RESERVED, saga.getPayload());
            }
            case STOCK_RESERVED -> {
                confirmOrder(orderId);
                saga.finish(COMPLETED, null);
            }
            default -> {
                return false;
            }
        }
        sagaRepository.save(saga);
        return saga.isRunning();
    }

    /** Compensation for a reservation that ran out of stock: void the hold, cancel the order. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensateStockShortage(UUID orderId, @Nullable String message) {
        SagaInstance saga = sagaRepository
                .findBySagaTypeAndCorrelationId(SAGA_TYPE, orderId)
                .orElse(null);
        if (saga == null || !saga.isRunning()) {
            return;
        }
        State state = read(saga.getPayload());
        if (state.authorizationId() != null) {
            paymentGateway.voidAuthorization(state.authorizationId());
        }
        String reason = "out of stock: " + message;
        cancelOrder(orderId, reason);
        saga.finish(COMPENSATED, reason);
        sagaRepository.save(saga);
    }

    private void authorizePayment(SagaInstance saga, State state) {
        PaymentAuthorization auth = paymentGateway.authorize(new PaymentAuthorizationRequest(
                state.orderId(),
                state.customerId(),
                state.amountMinorUnits(),
                state.currency(),
                "saga-" + saga.getId()));
        if (auth.approved()) {
            saga.transitionTo(PAYMENT_AUTHORIZED, write(state.withAuthorization(auth.authorizationId())));
        } else {
            String reason = "payment declined: " + auth.declineReason();
            cancelOrder(state.orderId(), reason);
            saga.finish(COMPENSATED, reason);
        }
    }

    private void confirmOrder(UUID orderId) {
        Order order = loadOrder(orderId);
        if (order.getStatus() == OrderStatus.PLACED) {
            order.confirm();
            orderRepository.save(order);
        }
    }

    private void cancelOrder(UUID orderId, String reason) {
        Order order = loadOrder(orderId);
        if (order.getStatus() == OrderStatus.DRAFT || order.getStatus() == OrderStatus.PLACED) {
            order.cancel();
            orderRepository.save(order);
        }
        log.info("saga cancelled order {}: {}", orderId, reason);
    }

    private Order loadOrder(UUID orderId) {
        return orderRepository
                .findDetailedById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
    }

    private String write(State state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise saga state for order " + state.orderId(), e);
        }
    }

    private State read(String payload) {
        try {
            return objectMapper.readValue(payload, State.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read saga state: " + payload, e);
        }
    }

    record State(
            UUID orderId, UUID customerId, long amountMinorUnits, String currency, @Nullable String authorizationId) {
        State withAuthorization(@Nullable String id) {
            return new State(orderId, customerId, amountMinorUnits, currency, id);
        }
    }
}
