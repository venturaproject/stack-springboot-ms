package com.boilerplate.application.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateApiClientCommand(@NotBlank String name, @NotEmpty List<String> scopes) {}
