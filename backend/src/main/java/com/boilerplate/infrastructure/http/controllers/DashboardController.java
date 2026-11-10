package com.boilerplate.infrastructure.http.controllers;

import com.boilerplate.application.dashboard.DashboardQueryService;
import com.boilerplate.application.dashboard.DashboardResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardQueryService dashboardQueryService;

    @GetMapping
    @PreAuthorize("hasAuthority('products:read') and hasAuthority('customers:read') and hasAuthority('orders:read')")
    public ResponseEntity<DashboardResponse> dashboard() {
        return ResponseEntity.ok(dashboardQueryService.query());
    }
}
