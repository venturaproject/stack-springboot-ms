package com.boilerplate.infrastructure.http.controllers;

import com.boilerplate.application.customers.CustomerListResponse;
import com.boilerplate.application.customers.CustomerQueryService;
import com.boilerplate.application.customers.CustomerResponse;
import com.boilerplate.application.customers.CustomerService;
import com.boilerplate.application.customers.RegisterCustomerCommand;
import com.boilerplate.application.customers.UpdateCustomerAddressCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer directory — name, unique email, optional address VO")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerQueryService customerQueryService;

    @GetMapping
    @PreAuthorize("hasAuthority('customers:read')")
    @Operation(summary = "List customers (paginated) with header stats")
    public ResponseEntity<CustomerListResponse> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean hasAddress) {
        return ResponseEntity.ok(customerQueryService.list(page, size, search, hasAddress));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customers:read')")
    public ResponseEntity<CustomerResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(customerQueryService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<CustomerResponse> register(@Valid @RequestBody RegisterCustomerCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.register(command));
    }

    @PatchMapping("/{id}/address")
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<CustomerResponse> updateAddress(
            @PathVariable UUID id, @Valid @RequestBody UpdateCustomerAddressCommand command) {
        return ResponseEntity.ok(customerService.updateAddress(id, command));
    }
}
