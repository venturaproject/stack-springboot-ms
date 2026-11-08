package com.boilerplate.application.dashboard;

/** Cross-aggregate figures for the dashboard header cards. */
public record DashboardResponse(
        long totalProducts,
        long lowStockProducts,
        long totalCustomers,
        long totalOrders,
        long draftOrders,
        long placedOrders,
        long cancelledOrders) {}
