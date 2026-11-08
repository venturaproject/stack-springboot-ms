package com.boilerplate.infrastructure.console;

import com.boilerplate.application.auth.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("cli")
@RequiredArgsConstructor
public class RoleListCommand implements ConsoleCommand {

    private final RoleService roleService;

    @Override
    public String getName() {
        return "role:list";
    }

    @Override
    public String getDescription() {
        return "List all available roles and their permissions";
    }

    @Override
    public void execute(ApplicationArguments args) {
        var roles = roleService.list();
        if (roles.isEmpty()) {
            System.out.println("No roles found.");
            return;
        }
        System.out.printf("  %-20s  %-40s  %s%n", "Name", "Description", "Permissions");
        System.out.println("-".repeat(90));
        roles.forEach(r -> System.out.printf(
                "  %-20s  %-40s  %s%n", r.name(), r.description(), String.join(", ", r.permissions())));
    }
}
