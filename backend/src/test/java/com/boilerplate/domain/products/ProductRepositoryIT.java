package com.boilerplate.domain.products;

import static org.assertj.core.api.Assertions.assertThat;

import com.boilerplate.domain.shared.Currency;
import com.boilerplate.domain.shared.Money;
import com.boilerplate.support.AbstractRepositoryIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class ProductRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    ProductRepository productRepository;

    private Product persisted(String sku, String name, int stock) {
        return productRepository.save(
                Product.create(Sku.of(sku), name, "desc", Money.fromMinorUnits(1000, Currency.USD), stock));
    }

    @Test
    void sku_is_an_embeddable_usable_for_lookup_and_search() {
        persisted("TSH-1001", "Blue Tee", 5);

        assertThat(productRepository.findBySku(Sku.of("tsh-1001"))).isPresent();
        assertThat(productRepository.search("blue", PageRequest.of(0, 10)).getContent())
                .extracting(p -> p.getSku().value())
                .contains("TSH-1001");
    }

    @Test
    void low_stock_count_uses_the_threshold() {
        long before = productRepository.countByStockQuantityLessThanEqual(5);
        persisted("LOW-1001", "Scarce", 1);
        persisted("BIG-1001", "Plenty", 999);

        assertThat(productRepository.countByStockQuantityLessThanEqual(5)).isEqualTo(before + 1);
    }

    @Test
    void the_price_columns_round_trip_as_money() {
        Product saved = persisted("MON-1001", "Priced", 3);

        Product reloaded = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getPrice()).isEqualTo(Money.fromMinorUnits(1000, Currency.USD));
    }
}
