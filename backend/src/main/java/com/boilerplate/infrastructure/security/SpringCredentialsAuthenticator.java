package com.boilerplate.infrastructure.security;

import com.boilerplate.application.auth.CredentialsAuthenticator;
import com.boilerplate.application.auth.LoginRateLimiter;
import com.boilerplate.config.AppProperties;
import com.boilerplate.domain.exception.AuthenticationFailedException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringCredentialsAuthenticator implements CredentialsAuthenticator {

    private final AuthenticationManager authenticationManager;
    private final LoginRateLimiter loginRateLimiter;
    private final AppProperties appProperties;

    @Override
    public void authenticate(String email, String rawPassword) {
        AppProperties.Security.RateLimit limits = appProperties.security().rateLimit();
        String accountKey = "account|" + (email == null ? "" : email.strip().toLowerCase(Locale.ROOT));
        if (!loginRateLimiter.tryAcquire(accountKey, limits.capacity(), limits.refillPerMinute())) {
            throw new AuthenticationFailedException("Invalid credentials");
        }
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, rawPassword));
        } catch (AuthenticationException e) {
            throw new AuthenticationFailedException("Invalid credentials");
        }
    }
}
