package com.boilerplate.infrastructure.console;

import com.boilerplate.application.auth.UserResponse;
import com.boilerplate.application.auth.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("cli")
@RequiredArgsConstructor
public class UserListCommand implements ConsoleCommand {

    private final UserService userService;

    @Override
    public String getName() {
        return "user:list";
    }

    @Override
    public String getDescription() {
        return "List all users";
    }

    @Override
    public void execute(ApplicationArguments args) {
        var page = userService.list(1, 50);
        if (page.data().isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        System.out.printf("%-36s  %-30s  %-25s  %s%n", "ID", "Email", "Name", "Roles");
        System.out.println("-".repeat(110));
        for (UserResponse u : page.data()) {
            System.out.printf(
                    "%-36s  %-30s  %-25s  %s%n",
                    u.id(), u.email(), u.firstName() + " " + u.lastName(), String.join(", ", u.roles()));
        }
        System.out.printf("%nTotal: %d%n", page.count());
    }
}
