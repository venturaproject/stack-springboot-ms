package com.boilerplate.infrastructure.http.controllers;

import com.boilerplate.application.api.ApiClientResponse;
import com.boilerplate.application.api.ApiClientService;
import com.boilerplate.application.api.CreateApiClientCommand;
import com.boilerplate.application.api.CreateApiClientResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/api-clients")
@RequiredArgsConstructor
@Tag(name = "API Clients", description = "Manage external API clients")
public class ApiClientController {

    private final ApiClientService apiClientService;

    @GetMapping
    @PreAuthorize("hasAuthority('api-clients:manage')")
    public ResponseEntity<List<ApiClientResponse>> list() {
        return ResponseEntity.ok(apiClientService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('api-clients:manage')")
    public ResponseEntity<CreateApiClientResult> create(@Valid @RequestBody CreateApiClientCommand cmd) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apiClientService.create(cmd));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('api-clients:manage')")
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        apiClientService.revoke(id);
        return ResponseEntity.noContent().build();
    }
}
