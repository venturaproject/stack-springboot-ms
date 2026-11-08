package com.boilerplate.application.products;

import com.boilerplate.application.PagedResponse;
import com.boilerplate.application.dispatch.QueryHandler;
import com.boilerplate.domain.products.Product;
import com.boilerplate.domain.products.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListProductsHandler implements QueryHandler<ListProductsQuery, PagedResponse<ProductResponse>> {

    private final ProductRepository productRepository;

    @Override
    public Class<ListProductsQuery> queryType() {
        return ListProductsQuery.class;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> handle(ListProductsQuery query) {
        var pageable =
                PageRequest.of(Math.max(0, query.page() - 1), query.size(), Sort.by(Sort.Direction.DESC, "createdAt"));
        String term = query.search() == null ? "" : query.search().trim();
        Page<Product> pageData = productRepository.search(term, pageable);
        return PagedResponse.from(pageData, ProductResponse::from);
    }
}
