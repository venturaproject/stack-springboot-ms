package com.boilerplate.domain.orders;

import static org.assertj.core.api.Assertions.assertThat;

import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.customers.Customer;
import com.boilerplate.domain.customers.CustomerRepository;
import com.boilerplate.domain.products.Sku;
import com.boilerplate.domain.shared.Currency;
import com.boilerplate.domain.shared.Money;
import com.boilerplate.support.AbstractRepositoryIT;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    CustomerRepository customerRepository;

    private UUID customerId;

    @BeforeEach
    void seedCustomer() {
        customerId = customerRepository
                .save(Customer.register("Order Owner", new Email("order-owner@example.com"), null))
                .getId();
    }

    private Order draftWithTwoLines() {
        Order order = Order.startDraft(customerId, null);
        order.addLine(UUID.randomUUID(), Sku.of("TSH-2001"), "Tee", Money.fromMinorUnits(1000, Currency.USD), 2);
        order.addLine(UUID.randomUUID(), Sku.of("MUG-2001"), "Mug", Money.fromMinorUnits(500, Currency.USD), 1);
        return order;
    }

    @Test
    void findDetailedById_loads_the_line_collection_and_the_persisted_total() {
        UUID id = orderRepository.save(draftWithTwoLines()).getId();

        Order loaded = orderRepository.findDetailedById(id).orElseThrow();

        assertThat(loaded.getLines()).hasSize(2);
        assertThat(loaded.getTotal()).isEqualTo(Money.fromMinorUnits(2500, Currency.USD));
    }

    @Test
    void countByStatus_reflects_a_placed_order() {
        long placedBefore = orderRepository.countByStatus(OrderStatus.PLACED);
        Order order = draftWithTwoLines();
        order.place();
        orderRepository.save(order);

        assertThat(orderRepository.countByStatus(OrderStatus.PLACED)).isEqualTo(placedBefore + 1);
    }

    @Test
    void findFirstByCustomerIdAndStatus_returns_the_draft_cart() {
        Order order = Order.startDraft(customerId, null);
        order.addLine(UUID.randomUUID(), Sku.of("STK-2001"), "Sticker", Money.fromMinorUnits(200, Currency.USD), 1);
        orderRepository.save(order);

        assertThat(orderRepository.findFirstByCustomerIdAndStatus(customerId, OrderStatus.DRAFT))
                .isPresent();
    }
}
