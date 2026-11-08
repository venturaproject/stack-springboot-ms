package com.boilerplate.infrastructure.security;

import com.boilerplate.application.auth.CredentialsAuthenticator;
import com.boilerplate.domain.exception.AuthenticationFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringCredentialsAuthenticator implements CredentialsAuthenticator {

    private final AuthenticationManager authenticationManager;

    @Override
    public void authenticate(String email, String rawPassword) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, rawPassword));
        } catch (AuthenticationException e) {
            throw new AuthenticationFailedException("Invalid credentials");
        }
    }
}
