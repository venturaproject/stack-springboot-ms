package com.boilerplate.application.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        List<String> scopes) {}
