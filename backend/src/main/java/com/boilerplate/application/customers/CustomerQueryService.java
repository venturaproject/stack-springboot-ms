package com.boilerplate.application.customers;

import com.boilerplate.domain.customers.Customer;
import com.boilerplate.domain.customers.CustomerRepository;
import com.boilerplate.domain.exception.EntityNotFoundException;
import jakarta.annotation.Nullable;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read use cases for the {@link Customer} aggregate. */
@Service
@RequiredArgsConstructor
public class CustomerQueryService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public CustomerListResponse list(int page, int size, @Nullable String search, @Nullable Boolean hasAddress) {
        var pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String term = search == null ? "" : search.trim();
        Page<Customer> pageData = customerRepository.search(term, hasAddress, pageable);
        long total = customerRepository.count();
        long withAddress = customerRepository.countWithAddress();
        var stats = new CustomerStatsResponse(total, withAddress, total - withAddress);
        var rows = pageData.getContent().stream().map(CustomerResponse::from).toList();
        return CustomerListResponse.from(pageData, rows, stats);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID id) {
        Customer customer = customerRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
        return CustomerResponse.from(customer);
    }
}
