package com.boilerplate.infrastructure.saga;

import com.boilerplate.domain.exception.EntityNotFoundException;
import com.boilerplate.domain.orders.Order;
import com.boilerplate.domain.orders.OrderLine;
import com.boilerplate.domain.orders.OrderRepository;
import com.boilerplate.domain.products.Product;
import com.boilerplate.domain.products.ProductRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reserves stock for every line of an order. Runs in the caller's transaction
 * ({@code MANDATORY}) — the saga step that calls it: if one line is short, the
 * {@link com.boilerplate.domain.exception.InsufficientStockException} propagates out and
 * the whole step's transaction rolls back, so the reservations already made for earlier
 * lines are undone and the saga can retry the step cleanly.
 */
@Component
@RequiredArgsConstructor
class StockReserver {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void reserveAll(UUID orderId) {
        Order order = orderRepository
                .findDetailedById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        for (OrderLine line : order.getLines()) {
            Product product = productRepository
                    .findById(line.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found: " + line.getProductId()));
            product.reserveStock(line.getQuantity(), "order_" + orderId);
            productRepository.save(product);
        }
    }
}
