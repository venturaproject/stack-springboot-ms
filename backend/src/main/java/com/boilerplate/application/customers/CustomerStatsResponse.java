package com.boilerplate.application.customers;

/** Aggregate figures shown alongside the customer list. */
public record CustomerStatsResponse(long total, long withAddress, long withoutAddress) {}
