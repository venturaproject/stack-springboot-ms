package com.boilerplate.domain.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boilerplate.domain.exception.BusinessRuleViolationException;
import com.boilerplate.domain.orders.event.OrderCancelled;
import com.boilerplate.domain.orders.event.OrderPlaced;
import com.boilerplate.domain.products.Sku;
import com.boilerplate.domain.shared.Currency;
import com.boilerplate.domain.shared.Money;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final Money PRICE = Money.fromMinorUnits(1000, Currency.USD);

    private static Order draftWithOneLine() {
        Order order = Order.startDraft(UUID.randomUUID(), null);
        order.addLine(UUID.randomUUID(), Sku.of("TSH-0001"), "Tee", PRICE, 2);
        return order;
    }

    @Test
    void total_is_derived_from_the_lines() {
        Order order = draftWithOneLine();
        assertThat(order.getTotal()).isEqualTo(Money.fromMinorUnits(2000, Currency.USD));

        order.addLine(UUID.randomUUID(), Sku.of("MUG-0001"), "Mug", Money.fromMinorUnits(500, Currency.USD), 1);
        assertThat(order.getTotal()).isEqualTo(Money.fromMinorUnits(2500, Currency.USD));
    }

    @Test
    void place_transitions_to_PLACED_and_records_an_OrderPlaced_event() {
        Order order = draftWithOneLine();
        order.clearEvents();

        order.place();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.recordedEvents()).singleElement().isInstanceOf(OrderPlaced.class);
    }

    @Test
    void a_placed_order_can_no_longer_be_modified() {
        Order order = draftWithOneLine();
        order.place();

        assertThatThrownBy(() -> order.addLine(UUID.randomUUID(), Sku.of("STK-0001"), "S", PRICE, 1))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void an_empty_order_cannot_be_placed() {
        Order order = Order.startDraft(UUID.randomUUID(), null);
        assertThatThrownBy(order::place).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void cancelling_twice_is_rejected() {
        Order order = draftWithOneLine();
        order.cancel();
        assertThat(order.recordedEvents()).anySatisfy(e -> assertThat(e).isInstanceOf(OrderCancelled.class));
        assertThatThrownBy(order::cancel).isInstanceOf(BusinessRuleViolationException.class);
    }
}
