package com.boilerplate.application.customers;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * The customer list plus its header stats in one payload. The pagination fields are flat
 * (same shape as {@code PagedResponse}) so the client reads one envelope, with {@code stats}
 * as a sibling key.
 */
public record CustomerListResponse(
        List<CustomerResponse> data, int page, int perPage, long count, int totalPages, CustomerStatsResponse stats) {
    public static CustomerListResponse from(
            Page<?> pageData, List<CustomerResponse> rows, CustomerStatsResponse stats) {
        return new CustomerListResponse(
                rows,
                pageData.getNumber() + 1,
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages(),
                stats);
    }
}
