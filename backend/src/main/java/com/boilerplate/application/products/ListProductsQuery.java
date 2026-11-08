package com.boilerplate.application.products;

import com.boilerplate.application.PagedResponse;
import com.boilerplate.application.dispatch.Query;
import jakarta.annotation.Nullable;

public record ListProductsQuery(int page, int size, @Nullable String search)
        implements Query<PagedResponse<ProductResponse>> {}
