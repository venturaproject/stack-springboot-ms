package com.boilerplate.infrastructure.http.controllers;

import com.boilerplate.application.auth.RoleResponse;
import com.boilerplate.application.auth.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Role management endpoints")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('roles:read')")
    @Operation(summary = "List all roles")
    public ResponseEntity<List<RoleResponse>> list() {
        return ResponseEntity.ok(roleService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('roles:read')")
    @Operation(summary = "Get role by ID")
    public ResponseEntity<RoleResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(roleService.getById(id));
    }
}
