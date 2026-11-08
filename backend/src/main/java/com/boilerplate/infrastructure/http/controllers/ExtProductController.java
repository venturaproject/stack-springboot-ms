package com.boilerplate.infrastructure.http.controllers;

import com.boilerplate.application.PagedResponse;
import com.boilerplate.application.dispatch.QueryBus;
import com.boilerplate.application.products.GetProductQuery;
import com.boilerplate.application.products.ListProductsQuery;
import com.boilerplate.application.products.ProductResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only projection of the catalog for external machine clients (scope {@code products:read}). */
@RestController
@RequestMapping("/api/ext/products")
@RequiredArgsConstructor
@Tag(name = "External API — Products")
public class ExtProductController {

    private final QueryBus queryBus;

    @GetMapping
    @PreAuthorize("hasAuthority('products:read')")
    public ResponseEntity<PagedResponse<ProductResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(queryBus.dispatch(new ListProductsQuery(page, size, search)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('products:read')")
    public ResponseEntity<ProductResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(queryBus.dispatch(new GetProductQuery(id)));
    }
}
