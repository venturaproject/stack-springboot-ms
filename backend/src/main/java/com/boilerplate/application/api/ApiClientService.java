package com.boilerplate.application.api;

import com.boilerplate.domain.api.ApiClient;
import com.boilerplate.domain.api.ApiClientRepository;
import com.boilerplate.domain.api.ApiClientToken;
import com.boilerplate.domain.api.ApiClientTokenRepository;
import com.boilerplate.domain.exception.AuthenticationFailedException;
import com.boilerplate.domain.exception.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiClientService {

    private static final long ACCESS_TTL_SECS = 3_600L;
    private static final long REFRESH_TTL_SECS = 2_592_000L;

    /** Scopes granted when a client is created with the {@code *} wildcard. */
    private static final List<String> ALL_SCOPES = List.of("products:read");

    private static final SecureRandom RNG = new SecureRandom();

    private final ApiClientRepository clientRepository;
    private final ApiClientTokenRepository tokenRepository;

    @Transactional(readOnly = true)
    public List<ApiClientResponse> list() {
        return clientRepository.findAll().stream().map(ApiClientResponse::from).toList();
    }

    @Transactional
    public CreateApiClientResult create(CreateApiClientCommand cmd) {
        String secret = randomHex(32);
        ApiClient client =
                ApiClient.issue(cmd.name(), "cli_" + randomHex(4), sha256(secret), cmd.scopes(), Instant.now());
        clientRepository.save(client);
        return new CreateApiClientResult(ApiClientResponse.from(client), secret);
    }

    @Transactional
    public void revoke(UUID id) {
        ApiClient client = clientRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("API client not found: " + id));
        client.revoke();
        tokenRepository.deleteByClientId(client.getClientId());
        clientRepository.save(client);
    }

    @Transactional
    public TokenResponse issueToken(IssueTokenCommand cmd) {
        ApiClient client = clientRepository
                .findByClientId(cmd.clientId())
                .filter(ApiClient::isActive)
                .filter(c -> c.secretMatches(sha256(cmd.clientSecret())))
                .orElseThrow(() -> new AuthenticationFailedException("Invalid credentials"));

        return buildTokenPair(client.getClientId(), expandScopes(client.getScopes()));
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenCommand cmd) {
        ApiClientToken old = tokenRepository
                .findByRefreshTokenHash(sha256(cmd.refreshToken()))
                .orElseThrow(() -> new AuthenticationFailedException("Invalid refresh token"));

        if (old.isRefreshExpired()) {
            throw new AuthenticationFailedException("Refresh token expired");
        }

        ApiClient client = clientRepository
                .findByClientId(old.getClientId())
                .filter(ApiClient::isActive)
                .orElseThrow(() -> new AuthenticationFailedException("Client is revoked"));

        tokenRepository.delete(old);
        return buildTokenPair(client.getClientId(), old.getScopes());
    }

    @Transactional
    public ApiTokenPrincipal validateAccessToken(String rawToken) {
        ApiClientToken token = tokenRepository
                .findByAccessTokenHash(sha256(rawToken))
                .orElseThrow(() -> new AuthenticationFailedException("Invalid token"));

        if (token.isAccessExpired()) {
            throw new AuthenticationFailedException("Token expired");
        }

        ApiClient client = clientRepository
                .findByClientId(token.getClientId())
                .filter(ApiClient::isActive)
                .orElseThrow(() -> new AuthenticationFailedException("Client is revoked"));

        client.recordUsage(Instant.now());
        clientRepository.save(client);

        return new ApiTokenPrincipal(token.getClientId(), token.getScopes());
    }

    private TokenResponse buildTokenPair(String clientId, List<String> scopes) {
        String access = randomHex(32);
        String refresh = randomHex(32);

        tokenRepository.save(ApiClientToken.issue(
                clientId, sha256(access), sha256(refresh), scopes, Instant.now(), ACCESS_TTL_SECS, REFRESH_TTL_SECS));

        return new TokenResponse(access, refresh, "Bearer", ACCESS_TTL_SECS, scopes);
    }

    private static List<String> expandScopes(List<String> scopes) {
        return scopes.contains("*") ? ALL_SCOPES : scopes;
    }

    private static String randomHex(int bytes) {
        byte[] buf = new byte[bytes];
        RNG.nextBytes(buf);
        return HexFormat.of().formatHex(buf);
    }

    private static String sha256(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
