package com.boilerplate.application.products;

import com.boilerplate.application.storage.ImageUpload;
import com.boilerplate.application.storage.MediaStoragePort;
import com.boilerplate.domain.exception.EntityNotFoundException;
import com.boilerplate.domain.products.Product;
import com.boilerplate.domain.products.ProductRepository;
import com.boilerplate.domain.shared.Currency;
import com.boilerplate.domain.shared.Money;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Write use cases for the {@link Product} aggregate. */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final MediaStoragePort mediaStorage;

    @Transactional
    public ProductResponse update(UUID id, UpdateProductCommand cmd) {
        Product product = load(id);
        product.updateDetails(cmd.name(), cmd.description());
        if (cmd.priceAmountMinorUnits() != null && cmd.priceCurrency() != null) {
            product.changePrice(Money.fromMinorUnits(cmd.priceAmountMinorUnits(), Currency.of(cmd.priceCurrency())));
        }
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse adjustStock(UUID id, AdjustStockCommand cmd) {
        Product product = load(id);
        if (cmd.delta() > 0) {
            product.restock(cmd.delta(), cmd.reasonOrDefault());
        } else if (cmd.delta() < 0) {
            product.reserveStock(-cmd.delta(), cmd.reasonOrDefault());
        } else {
            throw new IllegalArgumentException("Stock adjustment delta must not be zero");
        }
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse uploadImage(UUID id, ImageUpload image) {
        Product product = load(id);
        String previous = product.getImageFilename();
        String path = mediaStorage.storeProductImage(id, image);
        product.attachImage(path);
        ProductResponse response = ProductResponse.from(productRepository.save(product));
        if (previous != null && !previous.equals(path)) {
            mediaStorage.delete(previous);
        }
        return response;
    }

    @Transactional
    public ProductResponse removeImage(UUID id) {
        Product product = load(id);
        String previous = product.getImageFilename();
        product.removeImage();
        ProductResponse response = ProductResponse.from(productRepository.save(product));
        if (previous != null) {
            mediaStorage.delete(previous);
        }
        return response;
    }

    private Product load(UUID id) {
        return productRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    }
}
