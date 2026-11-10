package com.boilerplate.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.boilerplate.application.auth.LoginRateLimiter;
import com.boilerplate.config.AppProperties;
import com.boilerplate.domain.exception.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

@ExtendWith(MockitoExtension.class)
class SpringCredentialsAuthenticatorTest {

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    LoginRateLimiter loginRateLimiter;

    private SpringCredentialsAuthenticator authenticator;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(
                new AppProperties.Jwt("secret", 900, 604800),
                new AppProperties.Cookie(false, "Lax"),
                new AppProperties.Cors("http://localhost"),
                new AppProperties.Media("/tmp"),
                new AppProperties.Outbox(2000, 10, 1000, 300_000, 30),
                new AppProperties.Inbox(30),
                new AppProperties.Saga(5, 30),
                new AppProperties.Security(new AppProperties.Security.RateLimit(5, 5)));
        authenticator = new SpringCredentialsAuthenticator(authenticationManager, loginRateLimiter, properties);
    }

    @Test
    void applies_account_throttling_before_password_authentication() {
        when(loginRateLimiter.tryAcquire("account|person@example.com", 5, 5)).thenReturn(false);

        assertThatThrownBy(() -> authenticator.authenticate(" Person@Example.com ", "bad"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid credentials");

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void reaches_password_authentication_when_the_account_bucket_allows_it() {
        when(loginRateLimiter.tryAcquire("account|person@example.com", 5, 5)).thenReturn(true);

        authenticator.authenticate("Person@Example.com", "secret");

        verify(authenticationManager).authenticate(any());
    }
}
