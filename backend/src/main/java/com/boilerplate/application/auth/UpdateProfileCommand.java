package com.boilerplate.application.auth;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileCommand(@NotBlank String firstName, @NotBlank String lastName) {}
