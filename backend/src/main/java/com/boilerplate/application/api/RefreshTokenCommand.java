package com.boilerplate.application.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenCommand(@NotBlank @JsonProperty("refresh_token") String refreshToken) {}
