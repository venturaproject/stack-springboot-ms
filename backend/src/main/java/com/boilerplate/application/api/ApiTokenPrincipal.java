package com.boilerplate.application.api;

import java.util.List;

/**
 * The result of validating an external-API access token: who the caller is and what they
 * may do. Returned to the infrastructure filter instead of the {@code ApiClientToken}
 * entity so persistence types never leak out of the application layer.
 */
public record ApiTokenPrincipal(String clientId, List<String> scopes) {

    public ApiTokenPrincipal {
        scopes = List.copyOf(scopes);
    }
}
