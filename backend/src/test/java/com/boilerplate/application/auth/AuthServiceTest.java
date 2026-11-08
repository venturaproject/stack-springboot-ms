package com.boilerplate.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.boilerplate.config.AppProperties;
import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.auth.HashedPassword;
import com.boilerplate.domain.auth.RefreshToken;
import com.boilerplate.domain.auth.RefreshTokenRepository;
import com.boilerplate.domain.auth.User;
import com.boilerplate.domain.auth.UserRepository;
import com.boilerplate.domain.exception.AuthenticationFailedException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    CredentialsAuthenticator credentialsAuthenticator;

    @Mock
    UserRepository userRepository;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    CompromisedRefreshTokenHandler compromisedRefreshTokenHandler;

    @Mock
    AuthTokenIssuer tokenIssuer;

    private final AppProperties appProperties = new AppProperties(
            new AppProperties.Jwt("secret", 900, 604800),
            new AppProperties.Cookie(false, "Lax"),
            new AppProperties.Cors("*"),
            new AppProperties.Media("/tmp"),
            new AppProperties.Outbox(2000, 10, 1000, 300_000, 30),
            new AppProperties.Inbox(30),
            new AppProperties.Saga(5, 30),
            new AppProperties.Security(
                    new AppProperties.Security.RateLimit(5, 5), new AppProperties.Security.Lockout(5, 15, 15)));

    private AuthService service;

    private final User user =
            User.register(new Email("jane@x.com"), new HashedPassword("$stub"), "Jane", "Doe", Set.of());

    @BeforeEach
    void setUp() {
        service = new AuthService(
                credentialsAuthenticator,
                userRepository,
                refreshTokenRepository,
                compromisedRefreshTokenHandler,
                tokenIssuer,
                appProperties);
        lenient().when(tokenIssuer.generateAccessToken(any())).thenReturn("access");
        lenient().when(tokenIssuer.generateRefreshToken(any())).thenReturn("refresh");
        lenient().when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void login_authenticates_then_starts_a_token_family() {
        when(userRepository.findDetailedByEmail(any(Email.class))).thenReturn(Optional.of(user));

        LoginResult result = service.login(new LoginCommand("jane@x.com", "pw"));

        verify(credentialsAuthenticator).authenticate("jane@x.com", "pw");
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getFamilyId()).isNotNull();
        assertThat(result.tokens().accessToken()).isEqualTo("access");
    }

    @Test
    void refresh_rejects_an_unknown_token() {
        when(refreshTokenRepository.findByToken("raw")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refresh("raw")).isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void refresh_rotates_within_the_family() {
        RefreshToken stored =
                RefreshToken.startFamily("raw", user, Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByToken("raw")).thenReturn(Optional.of(stored));
        when(userRepository.findDetailedById(any())).thenReturn(Optional.of(user));

        LoginResult result = service.refresh("raw");

        assertThat(stored.isRevoked()).isTrue();
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getFamilyId()).isEqualTo(stored.getFamilyId());
        assertThat(result.tokens().accessToken()).isEqualTo("access");
    }

    @Test
    void refresh_with_a_revoked_token_triggers_the_breach_response_and_401() {
        RefreshToken stored =
                RefreshToken.startFamily("raw", user, Instant.now().plusSeconds(60));
        stored.revoke();
        when(refreshTokenRepository.findByToken("raw")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.refresh("raw"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("reuse");

        verify(compromisedRefreshTokenHandler).revokeFamilyAndFlagUser(eq(stored.getFamilyId()), any());
        verify(refreshTokenRepository, never()).save(any());
    }
}
