package com.boilerplate.application.products;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.boilerplate.domain.exception.DuplicateEntityException;
import com.boilerplate.domain.exception.InsufficientStockException;
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
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @InjectMocks
    ProductService service;

    @InjectMocks
    CreateProductHandler createProductHandler;

    private static Product tee(int stock) {
        return Product.create(Sku.of("TSH-0001"), "Tee", null, Money.fromMinorUnits(1999, Currency.USD), stock);
    }

    @Test
    void create_rejects_a_duplicate_sku() {
        when(productRepository.existsBySku(any(Sku.class))).thenReturn(true);

        assertThatThrownBy(() ->
                        createProductHandler.handle(new CreateProductCommand("TSH-0001", "Tee", null, 1999, "USD", 10)))
                .isInstanceOf(DuplicateEntityException.class);
    }

    @Test
    void adjustStock_with_a_negative_delta_reserves_and_can_run_out() {
        Product product = tee(2);
        when(productRepository.findById(any())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.adjustStock(UUID.randomUUID(), new AdjustStockCommand(-5, "shrinkage")))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void adjustStock_with_a_positive_delta_restocks() {
        Product product = tee(2);
        when(productRepository.findById(any())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse res = service.adjustStock(UUID.randomUUID(), new AdjustStockCommand(8, "restock"));

        assertThat(res.stockQuantity()).isEqualTo(10);
    }

    @Test
    void adjustStock_rejects_a_zero_delta() {
        Product product = tee(2);
        when(productRepository.findById(any())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.adjustStock(UUID.randomUUID(), new AdjustStockCommand(0, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
