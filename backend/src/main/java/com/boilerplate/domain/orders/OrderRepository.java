package com.boilerplate.domain.orders;

import jakarta.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Persistence port for the {@link Order} aggregate. */
public interface OrderRepository extends Repository<Order, UUID> {

    Optional<Order> findById(UUID id);

    /** Order with its lines fetched — for the detail view and every line mutation. */
    @EntityGraph(attributePaths = "lines")
    Optional<Order> findDetailedById(UUID id);

    /** The customer's current draft order, if any — the implicit "cart". */
    @EntityGraph(attributePaths = "lines")
    Optional<Order> findFirstByCustomerIdAndStatus(UUID customerId, OrderStatus status);

    @Query("SELECT o FROM Order o WHERE "
            + "(:customerId IS NULL OR o.customerId = :customerId) "
            + "AND (:status IS NULL OR o.status = :status)")
    Page<Order> filter(
            @Param("customerId") @Nullable UUID customerId,
            @Param("status") @Nullable OrderStatus status,
            Pageable pageable);

    long count();

    long countByStatus(OrderStatus status);

    Order save(Order order);
}
