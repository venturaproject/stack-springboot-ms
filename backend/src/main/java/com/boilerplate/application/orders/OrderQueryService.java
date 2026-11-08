package com.boilerplate.application.orders;

import com.boilerplate.application.PagedResponse;
import com.boilerplate.domain.exception.EntityNotFoundException;
import com.boilerplate.domain.orders.Order;
import com.boilerplate.domain.orders.OrderRepository;
import com.boilerplate.domain.orders.OrderStatus;
import jakarta.annotation.Nullable;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read use cases for the {@link Order} aggregate. */
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public PagedResponse<OrderSummaryResponse> list(
            int page, int size, @Nullable UUID customerId, @Nullable String status) {
        var pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        OrderStatus statusFilter = status != null && !status.isBlank()
                ? OrderStatus.valueOf(status.trim().toUpperCase(java.util.Locale.ROOT))
                : null;
        Page<Order> pageData = orderRepository.filter(customerId, statusFilter, pageable);
        return PagedResponse.from(pageData, OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID id) {
        Order order = orderRepository
                .findDetailedById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
        return OrderResponse.from(order);
    }
}
