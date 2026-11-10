package com.boilerplate.infrastructure.security;

import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.auth.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Resets legacy failed-login counters after a successful authentication. */
@Component
@RequiredArgsConstructor
class AuthenticationSuccessListener {

    private final UserRepository userRepository;

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
