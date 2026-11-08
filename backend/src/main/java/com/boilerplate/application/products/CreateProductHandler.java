package com.boilerplate.application.products;

import com.boilerplate.application.dispatch.CommandHandler;
import com.boilerplate.domain.exception.DuplicateEntityException;
import com.boilerplate.domain.products.Product;
import com.boilerplate.domain.products.ProductRepository;
import com.boilerplate.domain.products.Sku;
import com.boilerplate.domain.shared.Currency;
import com.boilerplate.domain.shared.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateProductHandler implements CommandHandler<CreateProductCommand, ProductResponse> {

    private final ProductRepository productRepository;

    @Override
    public Class<CreateProductCommand> commandType() {
        return CreateProductCommand.class;
    }

    @Override
    @Transactional
    public ProductResponse handle(CreateProductCommand command) {
        Sku sku = new Sku(command.sku());
        if (productRepository.existsBySku(sku)) {
            throw new DuplicateEntityException("A product with SKU " + sku.value() + " already exists");
        }
        Money price = Money.fromMinorUnits(command.priceAmountMinorUnits(), Currency.of(command.priceCurrency()));
        Product product = Product.create(sku, command.name(), command.description(), price, command.initialStock());
        return ProductResponse.from(productRepository.save(product));
    }
}
