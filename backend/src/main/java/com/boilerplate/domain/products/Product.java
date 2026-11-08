package com.boilerplate.domain.products;

import com.boilerplate.domain.exception.InsufficientStockException;
import com.boilerplate.domain.products.event.ProductCreated;
import com.boilerplate.domain.products.event.ProductPriceChanged;
import com.boilerplate.domain.products.event.StockAdjusted;
import com.boilerplate.domain.shared.AssignedIdentityAggregate;
import com.boilerplate.domain.shared.Currency;
import com.boilerplate.domain.shared.Money;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Product aggregate root — the "rich aggregate" reference alongside {@code Order}. Owns a
 * unique {@link Sku}, a {@link Money} price and a stock level that can never go negative:
 * {@link #reserveStock} is the only way stock leaves, and it refuses to over-reserve.
 * Price and stock changes each raise a domain event.
 */
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("NullAway.Init") // non-null fields are populated by Hibernate / factory methods
public class Product extends AssignedIdentityAggregate {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Nullable
    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "price_amount_minor_units", nullable = false)
    private long priceAmountMinorUnits;

    @Column(name = "price_currency", nullable = false, length = 3)
    private String priceCurrency;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Nullable
    @Column(name = "image_filename")
    private String imageFilename;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    private Product(UUID id, Sku sku, String name, @Nullable String description, Money price, int initialStock) {
        this.id = id;
        this.sku = sku.value();
        this.name = name;
        this.description = trimToNull(description);
        this.priceAmountMinorUnits = price.amountMinorUnits();
        this.priceCurrency = price.currency().name();
        this.stockQuantity = initialStock;
    }

    public static Product create(Sku sku, String name, @Nullable String description, Money price, int initialStock) {
        if (sku == null) {
            throw new IllegalArgumentException("SKU is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }
        if (price == null) {
            throw new IllegalArgumentException("Price is required");
        }
        if (initialStock < 0) {
            throw new IllegalArgumentException("Initial stock cannot be negative");
        }
        Product product = new Product(UUID.randomUUID(), sku, name.trim(), description, price, initialStock);
        product.recordEvent(new ProductCreated(product.id, product.sku, product.name));
        return product;
    }

    public void updateDetails(@Nullable String name, @Nullable String description) {
        if (name != null) {
            if (name.isBlank()) {
                throw new IllegalArgumentException("Product name must not be blank");
            }
            this.name = name.trim();
        }
        if (description != null) {
            this.description = trimToNull(description);
        }
    }

    public void changePrice(Money newPrice) {
        if (newPrice == null) {
            throw new IllegalArgumentException("Price is required");
        }
        Money current = getPrice();
        if (newPrice.equals(current)) {
            return;
        }
        this.priceAmountMinorUnits = newPrice.amountMinorUnits();
        this.priceCurrency = newPrice.currency().name();
        recordEvent(new ProductPriceChanged(
                id,
                current.amountMinorUnits(),
                newPrice.amountMinorUnits(),
                newPrice.currency().name()));
    }

    public void reserveStock(int quantity, String reason) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reserved quantity must be positive");
        }
        if (quantity > stockQuantity) {
            throw new InsufficientStockException(quantity, stockQuantity);
        }
        this.stockQuantity -= quantity;
        recordEvent(new StockAdjusted(id, -quantity, stockQuantity, reason));
    }

    public void restock(int quantity, String reason) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Restocked quantity must be positive");
        }
        this.stockQuantity += quantity;
        recordEvent(new StockAdjusted(id, quantity, stockQuantity, reason));
    }

    public void attachImage(String filename) {
        this.imageFilename = filename;
    }

    public void removeImage() {
        this.imageFilename = null;
    }

    @Nullable
    public String getImageFilename() {
        return imageFilename;
    }

    public Sku getSku() {
        return new Sku(sku);
    }

    public Money getPrice() {
        return Money.fromMinorUnits(priceAmountMinorUnits, Currency.of(priceCurrency));
    }

    @Nullable
    private static String trimToNull(@Nullable String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
