package com.boilerplate.infrastructure.security;

import com.boilerplate.application.auth.PasswordHasher;
import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.auth.Permission;
import com.boilerplate.domain.auth.PermissionRepository;
import com.boilerplate.domain.auth.Role;
import com.boilerplate.domain.auth.RoleRepository;
import com.boilerplate.domain.auth.User;
import com.boilerplate.domain.auth.UserRepository;
import jakarta.annotation.Nullable;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Runs before {@code SampleDataInitializer} — the admin user must exist first. */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordHasher passwordHasher;
    private final Environment environment;

    @Value("${app.bootstrap.admin-email:}")
    @Nullable
    private String bootstrapAdminEmail;

    @Value("${app.bootstrap.admin-password:}")
    @Nullable
    private String bootstrapAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Seeding initial data...");

        Permission usersRead = perm("users:read", "Read users");
        Permission usersWrite = perm("users:write", "Write users");
        Permission usersDelete = perm("users:delete", "Delete users");
        Permission rolesRead = perm("roles:read", "Read roles");
        Permission rolesWrite = perm("roles:write", "Manage roles");
        Permission apiClientsManage = perm("api-clients:manage", "Manage external API clients");
        Permission productsRead = perm("products:read", "Read the product catalog");
        Permission productsWrite = perm("products:write", "Create and edit products");
        Permission customersRead = perm("customers:read", "Read customers");
        Permission customersWrite = perm("customers:write", "Create and edit customers");
        Permission ordersRead = perm("orders:read", "Read orders");
        Permission ordersWrite = perm("orders:write", "Create and edit orders");
        Permission outboxManage = perm("outbox:manage", "Inspect and replay the outbox dead-letter queue");

        Role adminRole = roleRepository
                .findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.create(
                        "ADMIN",
                        "Administrator role with full access",
                        Set.of(
                                usersRead,
                                usersWrite,
                                usersDelete,
                                rolesRead,
                                rolesWrite,
                                apiClientsManage,
                                productsRead,
                                productsWrite,
                                customersRead,
                                customersWrite,
                                ordersRead,
                                ordersWrite,
                                outboxManage))));

        if (roleRepository.findByName("USER").isEmpty()) {
            roleRepository.save(Role.create("USER", "Standard user role", Set.of(productsRead)));
        }

        if (userRepository.count() == 0) {
            if (bootstrapAdminEmail == null
                    || bootstrapAdminEmail.isBlank()
                    || bootstrapAdminPassword == null
                    || bootstrapAdminPassword.isBlank()) {
                throw new IllegalStateException("No users exist. Configure ADMIN_BOOTSTRAP_EMAIL and "
                        + "ADMIN_BOOTSTRAP_PASSWORD to create the initial administrator.");
            }
            if (environment.acceptsProfiles(Profiles.of("prod")) && bootstrapAdminPassword.length() < 20) {
                throw new IllegalStateException(
                        "The initial production administrator password must be at least 20 characters");
            }
            userRepository.save(User.register(
                    new Email(bootstrapAdminEmail),
                    passwordHasher.hash(bootstrapAdminPassword),
                    "Admin",
                    "User",
                    Set.of(adminRole)));
            log.info("Created the initial administrator account");
        }

        log.info("Initial roles, permissions, and user setup complete");
    }

    private Permission perm(String name, String desc) {
        return permissionRepository
                .findByName(name)
                .orElseGet(() -> permissionRepository.save(Permission.create(name, desc)));
    }
}
