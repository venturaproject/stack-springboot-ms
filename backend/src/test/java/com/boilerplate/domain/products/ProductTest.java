package com.boilerplate.domain.products;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boilerplate.domain.exception.InsufficientStockException;
import com.boilerplate.domain.products.event.ProductCreated;
import com.boilerplate.domain.products.event.ProductPriceChanged;
import com.boilerplate.domain.products.event.StockAdjusted;
import com.boilerplate.domain.shared.Currency;
import com.boilerplate.domain.shared.DomainEvent;
import com.boilerplate.domain.shared.Money;
import org.junit.jupiter.api.Test;

class ProductTest {

    private static Product tee(int stock) {
        return Product.create(Sku.of("TSH-0001"), "Tee", "desc", Money.fromMinorUnits(1999, Currency.USD), stock);
    }

    @Test
    void create_records_a_ProductCreated_event() {
        Product product = tee(10);
        assertThat(product.recordedEvents()).singleElement().isInstanceOf(ProductCreated.class);
        assertThat(product.getId()).isNotNull();
    }

    @Test
    void reserveStock_reduces_stock_and_records_a_StockAdjusted_event() {
        Product product = tee(10);
        product.clearEvents();

        product.reserveStock(3, "order_placed");

        assertThat(product.getStockQuantity()).isEqualTo(7);
        assertThat(product.recordedEvents()).singleElement().isInstanceOf(StockAdjusted.class);
    }

    @Test
    void reserveStock_refuses_to_over_reserve() {
        Product product = tee(2);
        assertThatThrownBy(() -> product.reserveStock(3, "order_placed"))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(product.getStockQuantity()).isEqualTo(2);
    }

    @Test
    void changePrice_records_an_event_only_when_the_price_actually_changes() {
        Product product = tee(10);
        product.clearEvents();

        product.changePrice(Money.fromMinorUnits(1999, Currency.USD));
        assertThat(product.recordedEvents()).isEmpty();

        product.changePrice(Money.fromMinorUnits(2499, Currency.USD));
        assertThat(product.recordedEvents())
                .extracting(DomainEvent::eventType)
                .containsExactly(new ProductPriceChanged(product.getId(), 1999, 2499, "USD").eventType());
    }

    @Test
    void create_rejects_negative_initial_stock() {
        assertThatThrownBy(() -> tee(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
