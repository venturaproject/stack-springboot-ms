package com.boilerplate.domain.orders;

import com.boilerplate.domain.customers.Address;
import com.boilerplate.domain.exception.BusinessRuleViolationException;
import com.boilerplate.domain.exception.EntityNotFoundException;
import com.boilerplate.domain.orders.event.OrderCancelled;
import com.boilerplate.domain.orders.event.OrderConfirmed;
import com.boilerplate.domain.orders.event.OrderLineAdded;
import com.boilerplate.domain.orders.event.OrderPlaced;
import com.boilerplate.domain.products.Sku;
import com.boilerplate.domain.shared.AssignedIdentityAggregate;
import com.boilerplate.domain.shared.Currency;
import com.boilerplate.domain.shared.Money;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Order aggregate root. Owns its {@link OrderLine}s, references a {@code Customer} by id,
 * and carries an immutable shipping-address snapshot taken from that customer when the
 * draft starts. The total is always derived from the current lines, never set directly,
 * and only a {@code DRAFT} order can be modified.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("NullAway.Init") // non-null fields are populated by Hibernate / factory methods
public class Order extends AssignedIdentityAggregate {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Nullable
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "shipping_address_street")),
        @AttributeOverride(name = "city", column = @Column(name = "shipping_address_city")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "shipping_address_postal_code")),
        @AttributeOverride(name = "country", column = @Column(name = "shipping_address_country"))
    })
    private Address shippingAddress;

    @Nullable
    @Column(name = "total_amount_minor_units")
    private Long totalAmountMinorUnits;

    @Nullable
    @Column(name = "total_currency", length = 3)
    private String totalCurrency;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "line_index")
    private List<OrderLine> lines = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    private Order(UUID id, UUID customerId, @Nullable Address shippingAddress) {
        this.id = id;
        this.customerId = customerId;
        this.shippingAddress = shippingAddress;
        this.status = OrderStatus.DRAFT;
        this.lines = new ArrayList<>();
    }

    public static Order startDraft(UUID customerId, @Nullable Address shippingAddress) {
        if (customerId == null) {
            throw new IllegalArgumentException("customerId is required");
        }
        return new Order(UUID.randomUUID(), customerId, shippingAddress);
    }

    public void addLine(UUID productId, Sku productSku, String productName, Money unitPrice, int quantity) {
        requireDraft();
        if (quantity <= 0) {
            throw new IllegalArgumentException("Order line quantity must be positive");
        }
        lines.add(new OrderLine(this, productId, productSku, productName, unitPrice, quantity));
        recalculateTotal();
        recordEvent(new OrderLineAdded(id, productId, quantity));
    }

    public void changeLineQuantity(UUID lineId, int quantity) {
        requireDraft();
        findLine(lineId).changeQuantity(quantity);
        recalculateTotal();
    }

    public void removeLine(UUID lineId) {
        requireDraft();
        OrderLine line = findLine(lineId);
        lines.remove(line);
        recalculateTotal();
    }

    public void place() {
        requireDraft();
        if (lines.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot place an order with no lines");
        }
        this.status = OrderStatus.PLACED;
        Money total = requireTotal();
        recordEvent(new OrderPlaced(
                id, customerId, total.amountMinorUnits(), total.currency().name()));
    }

    /**
     * Fulfillment saga step: payment is authorised and stock reserved, so the order goes
     * ahead. Only a {@code PLACED} order can be confirmed; calling it again is a no-op-safe
     * error the saga guards against by checking the status first.
     */
    public void confirm() {
        if (status != OrderStatus.PLACED) {
            throw new BusinessRuleViolationException("Only a PLACED order can be confirmed (is " + status + ")");
        }
        this.status = OrderStatus.CONFIRMED;
        recordEvent(new OrderConfirmed(id, customerId));
    }

    public void cancel() {
        if (status == OrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException("Order is already cancelled");
        }
        if (status == OrderStatus.CONFIRMED) {
            throw new BusinessRuleViolationException("A confirmed order cannot be cancelled here — start a return");
        }
        this.status = OrderStatus.CANCELLED;
        recordEvent(new OrderCancelled(id));
    }

    public boolean isDraft() {
        return status == OrderStatus.DRAFT;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    @Nullable
    public Address getShippingAddress() {
        return shippingAddress;
    }

    @Nullable
    public Money getTotal() {
        if (totalAmountMinorUnits == null || totalCurrency == null) {
            return null;
        }
        return Money.fromMinorUnits(totalAmountMinorUnits, Currency.of(totalCurrency));
    }

    private void recalculateTotal() {
        Money total = null;
        for (OrderLine line : lines) {
            total = total == null ? line.getLineTotal() : total.add(line.getLineTotal());
        }
        this.totalAmountMinorUnits = total == null ? null : total.amountMinorUnits();
        this.totalCurrency = total == null ? null : total.currency().name();
    }

    private Money requireTotal() {
        Money total = getTotal();
        if (total == null) {
            throw new BusinessRuleViolationException("Cannot place an order with no total");
        }
        return total;
    }

    private OrderLine findLine(UUID lineId) {
        return lines.stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Order line not found: " + lineId));
    }

    private void requireDraft() {
        if (!isDraft()) {
            throw new BusinessRuleViolationException("Order is " + status + " and can no longer be modified");
        }
    }
}
