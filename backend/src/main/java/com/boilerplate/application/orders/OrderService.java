package com.boilerplate.application.orders;

import com.boilerplate.domain.customers.Customer;
import com.boilerplate.domain.customers.CustomerRepository;
import com.boilerplate.domain.exception.EntityNotFoundException;
import com.boilerplate.domain.orders.Order;
import com.boilerplate.domain.orders.OrderRepository;
import com.boilerplate.domain.products.Product;
import com.boilerplate.domain.products.ProductRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write use cases for the {@link Order} aggregate. {@link #place} only records the
 * customer's intent and emits {@code orders.placed.v1}; authorising payment and reserving
 * stock — the cross-aggregate, partially-remote work — is the {@code OrderFulfillmentSaga}'s
 * job, which drives the order to {@code CONFIRMED} or {@code CANCELLED} afterwards.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponse addLine(AddOrderLineCommand cmd) {
        Order order;
        if (cmd.orderId() != null) {
            order = loadDetailed(cmd.orderId());
        } else {
            if (cmd.customerId() == null) {
                throw new IllegalArgumentException("customerId is required to start a new order");
            }
            Customer customer = customerRepository
                    .findById(cmd.customerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + cmd.customerId()));
            order = Order.startDraft(customer.getId(), customer.getAddress());
        }

        Product product = productRepository
                .findById(cmd.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + cmd.productId()));

        order.addLine(product.getId(), product.getSku(), product.getName(), product.getPrice(), cmd.quantity());
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse changeLineQuantity(UUID orderId, UUID lineId, UpdateOrderLineQuantityCommand cmd) {
        Order order = loadDetailed(orderId);
        order.changeLineQuantity(lineId, cmd.quantity());
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse removeLine(UUID orderId, UUID lineId) {
        Order order = loadDetailed(orderId);
        order.removeLine(lineId);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse place(UUID orderId) {
        Order order = loadDetailed(orderId);
        order.place();
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancel(UUID orderId) {
        Order order = loadDetailed(orderId);
        order.cancel();
        return OrderResponse.from(orderRepository.save(order));
    }

    private Order loadDetailed(UUID id) {
        return orderRepository
                .findDetailedById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
    }
}
