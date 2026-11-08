package com.boilerplate.application.auth;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record UpdateUserCommand(
        @NotBlank String firstName, @NotBlank String lastName, boolean active, List<UUID> roleIds) {}
