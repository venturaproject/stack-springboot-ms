package com.boilerplate.application.products;

import com.boilerplate.application.dispatch.QueryHandler;
import com.boilerplate.domain.exception.EntityNotFoundException;
import com.boilerplate.domain.products.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetProductHandler implements QueryHandler<GetProductQuery, ProductResponse> {

    private final ProductRepository productRepository;

    @Override
    public Class<GetProductQuery> queryType() {
        return GetProductQuery.class;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse handle(GetProductQuery query) {
        return productRepository
                .findById(query.id())
                .map(ProductResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + query.id()));
    }
}
