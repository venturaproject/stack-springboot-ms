package com.boilerplate.infrastructure.saga;

import static org.assertj.core.api.Assertions.assertThat;

import com.boilerplate.application.orders.AddOrderLineCommand;
import com.boilerplate.application.orders.OrderResponse;
import com.boilerplate.application.orders.OrderService;
import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.customers.Customer;
import com.boilerplate.domain.customers.CustomerRepository;
import com.boilerplate.domain.orders.OrderRepository;
import com.boilerplate.domain.orders.OrderStatus;
import com.boilerplate.domain.products.Product;
import com.boilerplate.domain.products.ProductRepository;
import com.boilerplate.domain.products.Sku;
import com.boilerplate.domain.shared.Currency;
import com.boilerplate.domain.shared.Money;
import com.boilerplate.support.AbstractIntegrationIT;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end: place an order and let the fulfillment saga drive it to CONFIRMED, or
 * compensate to CANCELLED when payment is declined or stock is short. The saga runs
 * synchronously off the {@code AFTER_COMMIT} of {@code place()}, so by the time the call
 * returns it has settled.
 */
class OrderFulfillmentSagaIT extends AbstractIntegrationIT {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    SagaInstanceRepository sagaRepository;

    private UUID seedCustomer() {
        return customerRepository
                .save(Customer.register("Saga Buyer", new Email("saga-" + UUID.randomUUID() + "@example.com"), null))
                .getId();
    }

    private Product seedProduct(long priceMinorUnits, int stock) {
        String sku = "SAG-" + ThreadLocalRandom.current().nextInt(10_000, 99_999);
        return productRepository.save(Product.create(
                Sku.of(sku), "Saga Widget", null, Money.fromMinorUnits(priceMinorUnits, Currency.USD), stock));
    }

    private UUID place(UUID customerId, UUID productId, int qty) {
        OrderResponse draft = orderService.addLine(new AddOrderLineCommand(null, customerId, productId, qty));
        orderService.place(draft.id());
        return draft.id();
    }

    private SagaInstance saga(UUID orderId) {
        return sagaRepository
                .findBySagaTypeAndCorrelationId(OrderFulfillmentSaga.SAGA_TYPE, orderId)
                .orElseThrow();
    }

    @Test
    void happy_path_authorises_payment_reserves_stock_and_confirms_the_order() {
        UUID customerId = seedCustomer();
        Product product = seedProduct(1000, 5); // total 2000 -> approved

        UUID orderId = place(customerId, product.getId(), 2);

        assertThat(orderRepository.findDetailedById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(3);
        assertThat(saga(orderId).getState()).isEqualTo(OrderFulfillmentSaga.COMPLETED);
    }

    @Test
    void a_declined_payment_compensates_to_a_cancelled_order_and_leaves_stock_alone() {
        UUID customerId = seedCustomer();
        Product product = seedProduct(1300, 5); // total 1300 -> gateway declines (major units end in 3)

        UUID orderId = place(customerId, product.getId(), 1);

        assertThat(orderRepository.findDetailedById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(5);
        SagaInstance saga = saga(orderId);
        assertThat(saga.getState()).isEqualTo(OrderFulfillmentSaga.COMPENSATED);
        assertThat(saga.getLastError()).contains("declined");
    }

    @Test
    void running_out_of_stock_compensates_the_payment_and_cancels_the_order() {
        UUID customerId = seedCustomer();
        Product product = seedProduct(1000, 1); // total 2000 approved, but only 1 in stock for qty 2

        UUID orderId = place(customerId, product.getId(), 2);

        assertThat(orderRepository.findDetailedById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(1);
        assertThat(saga(orderId).getState()).isEqualTo(OrderFulfillmentSaga.COMPENSATED);
    }
}
