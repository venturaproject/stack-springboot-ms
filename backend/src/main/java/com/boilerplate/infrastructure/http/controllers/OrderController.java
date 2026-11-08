package com.boilerplate.infrastructure.http.controllers;

import com.boilerplate.application.PagedResponse;
import com.boilerplate.application.orders.AddOrderLineCommand;
import com.boilerplate.application.orders.OrderQueryService;
import com.boilerplate.application.orders.OrderResponse;
import com.boilerplate.application.orders.OrderService;
import com.boilerplate.application.orders.OrderSummaryResponse;
import com.boilerplate.application.orders.UpdateOrderLineQuantityCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Orders — aggregate with child lines, derived total, draft-only edits")
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    @GetMapping
    @PreAuthorize("hasAuthority('orders:read')")
    @Operation(summary = "List orders (paginated), optionally filtered by customer / status")
    public ResponseEntity<PagedResponse<OrderSummaryResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderQueryService.list(page, size, customerId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('orders:read')")
    public ResponseEntity<OrderResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(orderQueryService.getById(id));
    }

    @PostMapping("/lines")
    @PreAuthorize("hasAuthority('orders:write')")
    @Operation(summary = "Add a line — to an existing draft (orderId) or a new one (customerId)")
    public ResponseEntity<OrderResponse> addLine(@Valid @RequestBody AddOrderLineCommand command) {
        boolean created = command.orderId() == null;
        OrderResponse response = orderService.addLine(command);
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK)
                .body(response);
    }

    @PatchMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAuthority('orders:write')")
    public ResponseEntity<OrderResponse> updateLineQuantity(
            @PathVariable UUID id,
            @PathVariable UUID lineId,
            @Valid @RequestBody UpdateOrderLineQuantityCommand command) {
        return ResponseEntity.ok(orderService.changeLineQuantity(id, lineId, command));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAuthority('orders:write')")
    public ResponseEntity<OrderResponse> removeLine(@PathVariable UUID id, @PathVariable UUID lineId) {
        return ResponseEntity.ok(orderService.removeLine(id, lineId));
    }

    @PostMapping("/{id}/place")
    @PreAuthorize("hasAuthority('orders:write')")
    @Operation(summary = "Place the order — transitions to PLACED; the fulfillment saga then confirms or cancels it")
    public ResponseEntity<OrderResponse> place(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.place(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('orders:write')")
    public ResponseEntity<OrderResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }
}
