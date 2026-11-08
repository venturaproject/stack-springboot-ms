package com.boilerplate.domain.customers;

import com.boilerplate.domain.auth.Email;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Persistence port for the {@link Customer} aggregate. {@link Email} is unwrapped to its
 * {@code String} at this boundary so JPQL string functions work on it.
 */
public interface CustomerRepository extends Repository<Customer, UUID> {

    Optional<Customer> findById(UUID id);

    default Optional<Customer> findByEmail(Email email) {
        return findByEmailValue(email.value());
    }

    default boolean existsByEmail(Email email) {
        return existsByEmailValue(email.value());
    }

    @Query("SELECT c FROM Customer c WHERE LOWER(c.email) = LOWER(:email)")
    Optional<Customer> findByEmailValue(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM Customer c "
            + "WHERE LOWER(c.email) = LOWER(:email)")
    boolean existsByEmailValue(@Param("email") String email);

    List<Customer> findByIdIn(Collection<UUID> ids);

    /** {@code search} is never null — an empty string matches everything. */
    @Query("SELECT c FROM Customer c WHERE "
            + "(LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))) "
            + "AND (:hasAddress IS NULL "
            + "OR (:hasAddress = TRUE AND c.address.street IS NOT NULL) "
            + "OR (:hasAddress = FALSE AND c.address.street IS NULL))")
    Page<Customer> search(
            @Param("search") String search, @Param("hasAddress") @Nullable Boolean hasAddress, Pageable pageable);

    long count();

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.address.street IS NOT NULL")
    long countWithAddress();

    Customer save(Customer customer);
}
