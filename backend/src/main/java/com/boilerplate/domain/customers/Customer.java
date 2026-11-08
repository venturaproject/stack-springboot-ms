package com.boilerplate.domain.customers;

import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.customers.event.CustomerAddressChanged;
import com.boilerplate.domain.customers.event.CustomerRegistered;
import com.boilerplate.domain.shared.AssignedIdentityAggregate;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Customer aggregate root. Holds a name, a unique {@link Email} and an optional
 * {@link Address}. Registration and every address change raise a domain event; the
 * identity is assigned by the domain (not the database) so a {@code CustomerRegistered}
 * event can carry it before the row is flushed. The email is stored as a plain
 * {@code varchar} (VO wrapped/unwrapped at the boundary) so it stays searchable.
 */
@Entity
@Table(name = "customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("NullAway.Init") // non-null fields are populated by Hibernate / factory methods
public class Customer extends AssignedIdentityAggregate {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Nullable
    @Embedded
    private Address address;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    private Customer(UUID id, String name, Email email, @Nullable Address address) {
        this.id = id;
        this.name = name;
        this.email = email.value();
        this.address = address;
    }

    public static Customer register(String name, Email email, @Nullable Address address) {
        String normalized = requireName(name);
        if (email == null) {
            throw new IllegalArgumentException("Customer email is required");
        }
        Customer customer = new Customer(UUID.randomUUID(), normalized, email, address);
        customer.recordEvent(new CustomerRegistered(customer.id, email.value(), normalized));
        return customer;
    }

    public void rename(String name) {
        this.name = requireName(name);
    }

    public void changeAddress(Address newAddress) {
        if (newAddress == null) {
            throw new IllegalArgumentException("Address is required");
        }
        this.address = newAddress;
        recordEvent(new CustomerAddressChanged(id));
    }

    public boolean hasAddress() {
        return address != null;
    }

    public Email getEmail() {
        return new Email(email);
    }

    @Nullable
    public Address getAddress() {
        return address;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Customer name must not be blank");
        }
        return name.trim();
    }
}
