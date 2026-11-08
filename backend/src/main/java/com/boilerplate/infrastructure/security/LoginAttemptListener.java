package com.boilerplate.infrastructure.security;

import com.boilerplate.config.AppProperties;
import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.auth.UserRepository;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Translates Spring Security's authentication events into the account-lockout counters on
 * {@link com.boilerplate.domain.auth.User}. Runs in its own transaction so the counter
 * survives the rollback of the failed login.
 */
@Component
@RequiredArgsConstructor
class LoginAttemptListener {

    private final UserRepository userRepository;
    private final AppProperties appProperties;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
        emailOf(event).flatMap(userRepository::findByEmail).ifPresent(user -> {
            AppProperties.Security.Lockout cfg = appProperties.security().lockout();
            user.recordFailedLogin(
                    cfg.maxAttempts(), Duration.ofMinutes(cfg.windowMinutes()), Duration.ofMinutes(cfg.lockMinutes()));
            userRepository.save(user);
        });
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void onSuccess(AuthenticationSuccessEvent event) {
        emailOf(event).flatMap(userRepository::findByEmail).ifPresent(user -> {
            user.recordSuccessfulLogin();
            userRepository.save(user);
        });
    }

    private Optional<Email> emailOf(AbstractAuthenticationEvent event) {
        try {
            return Optional.of(
                    new Email(String.valueOf(event.getAuthentication().getName())));
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // an attempt against a non-address username — ignore
        }
    }
}
