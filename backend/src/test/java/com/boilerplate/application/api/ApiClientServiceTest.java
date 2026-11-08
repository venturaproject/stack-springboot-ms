package com.boilerplate.application.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.boilerplate.domain.api.ApiClient;
import com.boilerplate.domain.api.ApiClientRepository;
import com.boilerplate.domain.api.ApiClientToken;
import com.boilerplate.domain.api.ApiClientTokenRepository;
import com.boilerplate.domain.exception.AuthenticationFailedException;
import com.boilerplate.domain.exception.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiClientServiceTest {

    @Mock
    ApiClientRepository clientRepository;

    @Mock
    ApiClientTokenRepository tokenRepository;

    @InjectMocks
    ApiClientService service;

    private static String sha256(String s) throws Exception {
        return HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    }

    private ApiClient activeClient(String secret) throws Exception {
        return ApiClient.issue("bot", "cli_1", sha256(secret), List.of("products:read"), Instant.now());
    }

    @Test
    void issueToken_rejects_a_wrong_secret() throws Exception {
        when(clientRepository.findByClientId("cli_1")).thenReturn(Optional.of(activeClient("right")));

        assertThatThrownBy(() -> service.issueToken(new IssueTokenCommand("cli_1", "wrong")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void issueToken_rejects_a_revoked_client() throws Exception {
        ApiClient client = activeClient("right");
        client.revoke();
        when(clientRepository.findByClientId("cli_1")).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.issueToken(new IssueTokenCommand("cli_1", "right")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void issueToken_expands_the_wildcard_scope() throws Exception {
        ApiClient client = ApiClient.issue("bot", "cli_1", sha256("s"), List.of("*"), Instant.now());
        when(clientRepository.findByClientId("cli_1")).thenReturn(Optional.of(client));
        when(tokenRepository.save(any(ApiClientToken.class))).thenAnswer(inv -> inv.getArgument(0));

        TokenResponse res = service.issueToken(new IssueTokenCommand("cli_1", "s"));

        assertThat(res.tokenType()).isEqualTo("Bearer");
        assertThat(res.scopes()).contains("products:read");
    }

    @Test
    void validateAccessToken_rejects_an_unknown_token() {
        when(tokenRepository.findByAccessTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.validateAccessToken("nope")).isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void revoke_on_a_missing_client_is_a_404() {
        when(clientRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.revoke(UUID.randomUUID())).isInstanceOf(EntityNotFoundException.class);
    }
}
