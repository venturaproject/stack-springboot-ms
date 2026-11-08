package com.boilerplate.infrastructure.console;

import com.boilerplate.application.auth.CreateUserCommand;
import com.boilerplate.application.auth.UserResponse;
import com.boilerplate.application.auth.UserService;
import com.boilerplate.domain.auth.RoleRepository;
import com.boilerplate.domain.exception.DuplicateEntityException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("cli")
@RequiredArgsConstructor
public class UserCreateCommand implements ConsoleCommand {

    private final UserService userService;
    private final RoleRepository roleRepository;

    @Override
    public String getName() {
        return "user:create";
    }

    @Override
    public String getDescription() {
        return "Create a new user  --email --password --first-name --last-name [--role]";
    }

    @Override
    public void execute(ApplicationArguments args) {
        String email = required(args, "email");
        String password = required(args, "password");
        String firstName = required(args, "first-name");
        String lastName = required(args, "last-name");
        List<UUID> roleIds = resolveRoleIds(args);

        try {
            UserResponse user =
                    userService.create(new CreateUserCommand(email, password, firstName, lastName, roleIds));
            System.out.printf("User created: %s  id=%s  roles=%s%n", user.email(), user.id(), user.roles());
        } catch (DuplicateEntityException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    private String required(ApplicationArguments args, String name) {
        List<String> values = args.getOptionValues(name);
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Missing required option: --" + name);
        }
        return values.get(0);
    }

    private List<UUID> resolveRoleIds(ApplicationArguments args) {
        List<String> roleValues = args.getOptionValues("role");
        if (roleValues == null || roleValues.isEmpty()) {
            return List.of();
        }
        String roleName = roleValues.get(0).toUpperCase(Locale.ROOT);
        return roleRepository
                .findByName(roleName)
                .map(r -> List.of(r.getId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Role '" + roleName + "' not found. Run 'role:list' to see available roles."));
    }
}
