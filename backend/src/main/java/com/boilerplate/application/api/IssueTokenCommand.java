package com.boilerplate.application.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record IssueTokenCommand(
        @NotBlank @JsonProperty("client_id") String clientId,
        @NotBlank @JsonProperty("client_secret") String clientSecret) {}
