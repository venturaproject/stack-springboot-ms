package com.boilerplate.infrastructure.bootstrap;

import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.auth.UserRepository;
import com.boilerplate.domain.customers.Address;
import com.boilerplate.domain.customers.Customer;
import com.boilerplate.domain.customers.CustomerRepository;
import com.boilerplate.domain.notifications.Notification;
import com.boilerplate.domain.notifications.NotificationRepository;
import com.boilerplate.domain.orders.Order;
import com.boilerplate.domain.orders.OrderRepository;
import com.boilerplate.domain.products.Product;
import com.boilerplate.domain.products.ProductRepository;
import com.boilerplate.domain.products.Sku;
import com.boilerplate.domain.shared.Currency;
import com.boilerplate.domain.shared.Money;
import jakarta.annotation.Nullable;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demo data for the example e-commerce domain (products, customers, one draft order,
 * a couple of notifications). Dev profile only — production bootstrapping (the admin
 * user, roles, permissions) lives in {@code infrastructure.security.DataInitializer}.
 */
@Component
@Profile("dev")
@org.springframework.core.annotation.Order(2) // after DataInitializer, which creates the admin user
@RequiredArgsConstructor
@Slf4j
public class SampleDataInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Value("${app.bootstrap.admin-email:}")
    @Nullable
    private String bootstrapAdminEmail;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            return;
        }
        log.info("Seeding sample catalog...");

        Product tshirt = productRepository.save(Product.create(
                Sku.of("TSH-0001"),
                "Classic T-Shirt",
                "100% combed cotton, unisex fit.",
                Money.fromMinorUnits(1999, Currency.USD),
                120));
        Product mug = productRepository.save(Product.create(
                Sku.of("MUG-0001"),
                "Ceramic Mug",
                "350ml, dishwasher safe.",
                Money.fromMinorUnits(1249, Currency.USD),
                3));
        productRepository.save(Product.create(
                Sku.of("STK-0001"),
                "Sticker Pack",
                "Set of 10 die-cut vinyl stickers.",
                Money.fromMinorUnits(599, Currency.USD),
                400));

        Customer alice = customerRepository.save(Customer.register(
                "Alice Johnson",
                new Email("alice@example.com"),
                new Address("221B Baker Street", "London", "NW1 6XE", "United Kingdom")));
        customerRepository.save(Customer.register("Bob Smith", new Email("bob@example.com"), null));

        Order draft = Order.startDraft(alice.getId(), alice.getAddress());
        draft.addLine(tshirt.getId(), tshirt.getSku(), tshirt.getName(), tshirt.getPrice(), 2);
        draft.addLine(mug.getId(), mug.getSku(), mug.getName(), mug.getPrice(), 1);
        orderRepository.save(draft);

        UUID adminId = adminId();
        notificationRepository.save(
                Notification.create(adminId, "Welcome", "This is a sample notification for the admin user.", "info"));
        notificationRepository.save(
                Notification.create(null, "Low stock", "Ceramic Mug is running low (3 left).", "warning"));

        log.info(
                "Seeded {} products, {} customers, 1 draft order",
                productRepository.count(),
                customerRepository.count());
    }

    @Nullable
    private UUID adminId() {
        if (bootstrapAdminEmail == null || bootstrapAdminEmail.isBlank()) {
            return null;
        }
        return userRepository
                .findByEmail(new Email(bootstrapAdminEmail))
                .map(u -> u.getId())
                .orElse(null);
    }
}
