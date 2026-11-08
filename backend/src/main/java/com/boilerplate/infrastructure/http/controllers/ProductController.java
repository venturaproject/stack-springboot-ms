package com.boilerplate.infrastructure.http.controllers;

import com.boilerplate.application.PagedResponse;
import com.boilerplate.application.dispatch.CommandBus;
import com.boilerplate.application.dispatch.QueryBus;
import com.boilerplate.application.products.AdjustStockCommand;
import com.boilerplate.application.products.CreateProductCommand;
import com.boilerplate.application.products.GetProductQuery;
import com.boilerplate.application.products.ListProductsQuery;
import com.boilerplate.application.products.ProductResponse;
import com.boilerplate.application.products.ProductService;
import com.boilerplate.application.products.UpdateProductCommand;
import com.boilerplate.application.storage.ImageUpload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Catalog — a rich aggregate (unique SKU, Money price, non-negative stock)")
public class ProductController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAuthority('products:read')")
    @Operation(summary = "List products (paginated)")
    public ResponseEntity<PagedResponse<ProductResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(queryBus.dispatch(new ListProductsQuery(page, size, search)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('products:read')")
    public ResponseEntity<ProductResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(queryBus.dispatch(new GetProductQuery(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commandBus.dispatch(command));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<ProductResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateProductCommand command) {
        return ResponseEntity.ok(productService.update(id, command));
    }

    @PostMapping("/{id}/stock-adjustments")
    @PreAuthorize("hasAuthority('products:write')")
    @Operation(summary = "Restock (positive delta) or reserve (negative delta) stock")
    public ResponseEntity<ProductResponse> adjustStock(
            @PathVariable UUID id, @Valid @RequestBody AdjustStockCommand command) {
        return ResponseEntity.ok(productService.adjustStock(id, command));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<ProductResponse> uploadImage(
            @PathVariable UUID id, @RequestPart("image") MultipartFile file) {
        return ResponseEntity.ok(productService.uploadImage(id, toImageUpload(file)));
    }

    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<ProductResponse> removeImage(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.removeImage(id));
    }

    private static ImageUpload toImageUpload(MultipartFile file) {
        try {
            return new ImageUpload(file.getBytes(), file.getOriginalFilename(), file.getContentType());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read uploaded file", e);
        }
    }
}
