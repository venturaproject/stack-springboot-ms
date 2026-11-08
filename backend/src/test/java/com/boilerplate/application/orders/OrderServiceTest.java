package com.boilerplate.application.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.customers.Customer;
import com.boilerplate.domain.customers.CustomerRepository;
import com.boilerplate.domain.exception.EntityNotFoundException;
import com.boilerplate.domain.orders.Order;
import com.boilerplate.domain.orders.OrderRepository;
import com.boilerplate.domain.products.Product;
import com.boilerplate.domain.products.ProductRepository;
import com.boilerplate.domain.products.Sku;
import com.boilerplate.domain.shared.Currency;
import com.boilerplate.domain.shared.Money;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    CustomerRepository customerRepository;

    @Mock
    ProductRepository productRepository;

    @InjectMocks
    OrderService service;

    private static Product tee(int stock) {
        return Product.create(Sku.of("TSH-0001"), "Tee", null, Money.fromMinorUnits(1000, Currency.USD), stock);
    }

    @Test
    void addLine_without_an_order_starts_a_draft_for_the_customer() {
        Customer customer = Customer.register("Alice", new Email("alice@example.com"), null);
        Product product = tee(10);
        UUID productId = product.getId();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse res = service.addLine(new AddOrderLineCommand(null, customer.getId(), productId, 2));

        assertThat(res.status()).isEqualTo("DRAFT");
        assertThat(res.lines()).hasSize(1);
        assertThat(res.totalAmountMinorUnits()).isEqualTo(2000);
    }

    @Test
    void addLine_requires_a_customer_when_no_order_is_given() {
        assertThatThrownBy(() -> service.addLine(new AddOrderLineCommand(null, null, UUID.randomUUID(), 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void place_only_transitions_the_order_and_records_intent() {
        Product product = tee(1);
        Order order = Order.startDraft(UUID.randomUUID(), null);
        order.addLine(product.getId(), product.getSku(), product.getName(), product.getPrice(), 1);
        when(orderRepository.findDetailedById(any())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse res = service.place(UUID.randomUUID());

        // stock is untouched here — the fulfillment saga reserves it after OrderPlaced commits
        assertThat(res.status()).isEqualTo("PLACED");
        assertThat(product.getStockQuantity()).isEqualTo(1);
    }

    @Test
    void addLine_on_a_missing_order_is_a_404() {
        when(orderRepository.findDetailedById(any())).thenReturn(Optional.empty());
        var cmd = new AddOrderLineCommand(UUID.randomUUID(), null, UUID.randomUUID(), 1);

        assertThatThrownBy(() -> service.addLine(cmd)).isInstanceOf(EntityNotFoundException.class);
    }
}
