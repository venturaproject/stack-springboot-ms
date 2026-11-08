package com.boilerplate.application.auth;

import com.boilerplate.domain.exception.AuthenticationFailedException;

/**
 * Outbound port: verifies an email/password pair. The Spring Security
 * {@code AuthenticationManager} plumbing is an infrastructure adapter.
 */
public interface CredentialsAuthenticator {

    /**
     * @throws AuthenticationFailedException if the credentials are not valid
     */
    void authenticate(String email, String rawPassword);
}
