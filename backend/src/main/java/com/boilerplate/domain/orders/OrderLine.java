package com.boilerplate.domain.orders;

import com.boilerplate.domain.products.Sku;
import com.boilerplate.domain.shared.Currency;
import com.boilerplate.domain.shared.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Child entity of the {@link Order} aggregate — it has its own identity and row, but no
 * repository: created, mutated and removed only through its {@code Order}. Snapshots the
 * product's SKU, name and unit price at add-time so a later catalog change never rewrites
 * a placed order's history.
 */
@Entity
@Table(name = "order_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("NullAway.Init") // non-null fields are populated by Hibernate / the Order aggregate
public class OrderLine {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_sku", nullable = false)
    private String productSku;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "unit_price_amount_minor_units", nullable = false)
    private long unitPriceAmountMinorUnits;

    @Column(name = "unit_price_currency", nullable = false, length = 3)
    private String unitPriceCurrency;

    @Column(nullable = false)
    private int quantity;

    OrderLine(Order order, UUID productId, Sku productSku, String productName, Money unitPrice, int quantity) {
        this.id = UUID.randomUUID();
        this.order = order;
        this.productId = productId;
        this.productSku = productSku.value();
        this.productName = productName;
        this.unitPriceAmountMinorUnits = unitPrice.amountMinorUnits();
        this.unitPriceCurrency = unitPrice.currency().name();
        this.quantity = quantity;
    }

    void changeQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Order line quantity must be positive");
        }
        this.quantity = quantity;
    }

    public Sku getProductSku() {
        return new Sku(productSku);
    }

    public Money getUnitPrice() {
        return Money.fromMinorUnits(unitPriceAmountMinorUnits, Currency.of(unitPriceCurrency));
    }

    public Money getLineTotal() {
        return getUnitPrice().multiply(quantity);
    }
}
