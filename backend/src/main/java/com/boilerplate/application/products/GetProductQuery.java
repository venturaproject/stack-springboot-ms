package com.boilerplate.application.products;

import com.boilerplate.application.dispatch.Query;
import java.util.UUID;

public record GetProductQuery(UUID id) implements Query<ProductResponse> {}
